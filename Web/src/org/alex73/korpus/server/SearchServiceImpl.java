package org.alex73.korpus.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.base.BelarusianWordNormalizer;
import org.alex73.korpus.server.data.ClusterParams;
import org.alex73.korpus.server.data.ClusterResults;
import org.alex73.korpus.server.data.CorpusType;
import org.alex73.korpus.server.data.InitialData;
import org.alex73.korpus.server.data.LatestMark;
import org.alex73.korpus.server.data.ResultText;
import org.alex73.korpus.server.data.SearchParams;
import org.alex73.korpus.server.data.SearchResult;
import org.alex73.korpus.server.data.SearchResults;
import org.alex73.korpus.server.data.WordRequest;
import org.alex73.korpus.server.data.WordResult;
import org.alex73.korpus.server.engine.LuceneDriverRead;
import org.alex73.korpus.server.text.BinaryParagraphReader;
import org.alex73.korpus.text.xml.O;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.S;
import org.alex73.korpus.text.xml.Se;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.Z;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;

@Path("/korpus")
public class SearchServiceImpl {
    private final static Logger LOGGER = Logger.getLogger(SearchServiceImpl.class.getName());

    @Context
    HttpServletRequest request;
    
    private KorpusApplication getApp() {
        return KorpusApplication.instance;
    }

    @Path("initial")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public InitialData getInitialData() throws Exception {
        LOGGER.info("getInitialData from " + request.getRemoteAddr());
        try {
            return getApp().searchInitial;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "getInitialData", ex);
            throw ex;
        }
    }

    public static class SearchRequest {
        public SearchParams params;
        public LatestMark latest;
    }

    @Path("search")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SearchResult search(SearchRequest rq) throws Exception {
        LOGGER.info(">> Request from " + request.getRemoteAddr());
        SearchParams params = rq.params;
        LatestMark latest = rq.latest;
        for (WordRequest w : params.words) {
            w.word = BelarusianWordNormalizer.normalize(w.word);
        }
        try {
            WordsDetailsChecks.reset();
            boolean enoughComplex = false;
            for (WordRequest w : params.words) {
                if (!WordsDetailsChecks.isTooSimpleWord(w)) {
                    enoughComplex = true;
                    break;
                }
            }
            if (!enoughComplex) {
                LOGGER.info("<< Request too simple");
                throw new RuntimeException(ServerError.REQUIEST_TOO_SIMPLE);
            }
            for (WordRequest w : params.words) {
                if (w.allForms) {
                    w.lemmas = findAllLemmas(w.word);
                    if (w.lemmas.isEmpty()) {
                        throw new RuntimeException(ServerError.REQUIEST_LEMMA_NOT_FOUND);
                    }
                }
            }

            BooleanQuery query = new BooleanQuery();
            LuceneFilter process = getProcess(params.corpusType);
            if (params.corpusType == CorpusType.MAIN) {
                process.addKorpusTextFilter(query, params.textStandard);
            } else {
                process.addOtherTextFilter(query, params.textUnprocessed);
            }

            for (WordRequest w : params.words) {
                process.addWordFilter(query, w);
            }

            if (latest == null) {
                latest = new LatestMark();
            }
            List<Integer> found = process.search(query, latest, Settings.KORPUS_SEARCH_RESULT_PAGE,
                    new LuceneDriverRead.DocFilter<Integer>() {
                        public Integer processDoc(int docID) {
                            try {
                                Document doc = getProcess(params.corpusType).getSentence(docID);
                                ResultText text = restoreText(params.corpusType, doc);
                                if (WordsDetailsChecks.isAllowed(params.wordsOrder, params.words, text)) {
                                    return docID;
                                } else {
                                    return null;
                                }
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    });
            SearchResult result = new SearchResult();
            result.hasMore = found.size() >= Settings.KORPUS_SEARCH_RESULT_PAGE;
            result.foundIDs = new int[found.size()];
            for (int i = 0; i < result.foundIDs.length; i++) {
                result.foundIDs[i] = found.get(i);
            }
            result.latest = latest;
            LOGGER.info("<< Result: found: " + result.foundIDs.length + " hasMore:" + result.hasMore);
            return result;
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "<< Result error", ex);
            SearchResult result = new SearchResult();
            result.error = ex.getMessage();
            return result;
        }
    }
    
    @Path("cluster")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ClusterResults calculateClusters(ClusterParams params) throws Exception {
        LOGGER.info(">> Request clusters from " + request.getRemoteAddr());
        params.word.word = BelarusianWordNormalizer.normalize(params.word.word);
        try {
            WordsDetailsChecks.reset();
            if (WordsDetailsChecks.isTooSimpleWord(params.word)) {
                LOGGER.info("<< Request too simple");
                throw new RuntimeException(ServerError.REQUIEST_TOO_SIMPLE);
            }
            if (params.word.allForms) {
                params.word.lemmas = findAllLemmas(params.word.word);
                if (params.word.lemmas.isEmpty()) {
                    throw new RuntimeException(ServerError.REQUIEST_LEMMA_NOT_FOUND);
                }
            }

            LuceneFilter corpusFilter = getProcess(params.corpusType);
            ClusterResults res = new ClusterServiceImpl(this).calc(params, corpusFilter);
            LOGGER.info("<< Result clusters");
            return res;
        } catch (Throwable ex) {
            LOGGER.info("<< Result error: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public static class SentencesRequest {
        public SearchParams params;
        public int[] list;
    }

    @Path("sentences")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SearchResults[] getSentences(SentencesRequest rq) throws Exception {
        SearchParams params = rq.params;
        int[] list = rq.list;
        for (WordRequest w : params.words) {
            w.word = BelarusianWordNormalizer.normalize(w.word);
        }
        for (WordRequest w : params.words) {
            if (w.allForms) {
                w.lemmas = findAllLemmas(w.word);
                if (w.lemmas.isEmpty()) {
                    throw new RuntimeException(ServerError.REQUIEST_LEMMA_NOT_FOUND);
                }
            }
        }
        try {
            WordsDetailsChecks.reset();
            SearchResults[] result = new SearchResults[list.length];
            for (int i = 0; i < list.length; i++) {
                Document doc = getProcess(params.corpusType).getSentence(list[i]);
                result[i] = new SearchResults();
                restoreTextInfo(params, doc, result[i]);
                result[i].text = restoreText(params.corpusType, doc);
                // mark result words
                WordsDetailsChecks.isAllowed(params.wordsOrder, params.words, result[i].text);
            }
            return result;
        } catch (Exception ex) {
            LOGGER.info("<< Result error: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

   

    protected void restoreTextInfo(SearchParams params, Document doc, SearchResults result) throws Exception {
        if (params.corpusType == CorpusType.MAIN) {
            result.doc = getApp().processKorpus.getKorpusTextInfo(doc);
        } else {
            result.docOther = getApp().processOther.getOtherInfo(doc);
        }
    }

    protected ResultText restoreText(CorpusType corpusType, Document doc) throws Exception {
        LuceneFilter process=getProcess(corpusType);

        byte[] xml = process.getXML(doc);

        P paragraph = new BinaryParagraphReader(xml).read();

        List<WordResult[]> sentences = new ArrayList<>();

        for (int i = 0; i < paragraph.getSe().size(); i++) {
            Se sentence = paragraph.getSe().get(i);
            List<WordResult> words = new ArrayList<>();
            for (int j = 0; j < sentence.getWOrSOrZ().size(); j++) {
                Object o = sentence.getWOrSOrZ().get(j);
                WordResult rsw = new WordResult();
                if (o instanceof W) {
                    rsw.orig = ((W) o).getValue();
                    rsw.normalized = BelarusianWordNormalizer.normalize(rsw.orig); // TODO may be store instead convert
                                                                                   // each time ?
                    rsw.lemma = ((W) o).getLemma();
                    rsw.cat = ((W) o).getCat();
                    rsw.isWord = true;
                } else if (o instanceof S) {
                    rsw.orig = ((S) o).getChar();
                } else if (o instanceof Z) {
                    rsw.orig = ((Z) o).getChar();
                } else if (o instanceof O) {
                    rsw.orig = ((O) o).getValue();
                }
                words.add(rsw);
            }
            if (!words.isEmpty()) {
                sentences.add(words.toArray(new WordResult[0]));
            }
        }
        ResultText text = new ResultText();
        text.words = sentences.toArray(new WordResult[0][]);

        return text;
    }

    private List<String> findAllLemmas(String word) {
        word = BelarusianWordNormalizer.normalize(word);
        Set<String> result = new HashSet<>();
        Paradigm[] ps = getApp().grFinder.getParadigmsLikeForm(word);
        nextp: for (Paradigm p : ps) {
            for (Variant v : p.getVariant()) {
                for (Form f : v.getForm()) {
                    if (BelarusianWordNormalizer.normalize(f.getValue()).equals(word)) {
                        result.add(p.getLemma());
                        continue nextp;
                    }
                }
            }
        }
        return new ArrayList<>(result);
    }

    private LuceneFilter getProcess(CorpusType corpusType) {
        if (corpusType == CorpusType.MAIN) {
            return getApp().processKorpus;
        } else {
            return getApp().processOther;
        }
    }
}
