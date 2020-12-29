package org.alex73.korpus.server;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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

import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.base.BelarusianWordNormalizer;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.server.data.ClusterParams;
import org.alex73.korpus.server.data.ClusterResults;
import org.alex73.korpus.server.data.InitialData;
import org.alex73.korpus.server.data.LatestMark;
import org.alex73.korpus.server.data.SearchParams;
import org.alex73.korpus.server.data.SearchResult;
import org.alex73.korpus.server.data.SearchResults;
import org.alex73.korpus.server.data.WordRequest;
import org.alex73.korpus.server.data.WordResult;
import org.alex73.korpus.server.engine.LuceneDriverRead;
import org.alex73.korpus.server.text.BinaryParagraphReader;
import org.alex73.korpus.text.elements.Paragraph;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;

//TODO Пошук па слове з улікам першага "ў" і вялікіх літар
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
            if (w.word != null) {
                w.word = BelarusianWordNormalizer.lightNormalized(w.word);
            }
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
                    findAllLemmas(w);
                    if (w.lemmas.length == 0) {
                        throw new RuntimeException(ServerError.REQUIEST_LEMMA_NOT_FOUND);
                    }
                }
            }

            BooleanQuery.Builder query = new BooleanQuery.Builder();
            LuceneFilter process = getApp().processKorpus;
            process.addKorpusTextFilter(query, params.textStandard);
            for (WordRequest w : params.words) {
                process.addWordFilter(query, w);
            }

            if (latest == null) {
                latest = new LatestMark();
            }
            List<Integer> found = process.search(query.build(), latest, Settings.KORPUS_SEARCH_RESULT_PAGE,
                    new LuceneDriverRead.DocFilter<Integer>() {
                        public Integer processDoc(int docID) {
                            try {
                                Document doc = getApp().processKorpus.getSentence(docID);
                                Paragraph text = restoreText(doc);
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
        params.word.word = BelarusianWordNormalizer.lightNormalized(params.word.word);
        try {
            WordsDetailsChecks.reset();
            if (WordsDetailsChecks.isTooSimpleWord(params.word)) {
                LOGGER.info("<< Request too simple");
                throw new RuntimeException(ServerError.REQUIEST_TOO_SIMPLE);
            }
            if (params.word.allForms) {
                findAllLemmas(params.word);
                if (params.word.lemmas.length == 0) {
                    throw new RuntimeException(ServerError.REQUIEST_LEMMA_NOT_FOUND);
                }
            }

            LuceneFilter corpusFilter = getApp().processKorpus;
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
            w.word = BelarusianWordNormalizer.lightNormalized(w.word);
        }
        for (WordRequest w : params.words) {
            if (w.allForms) {
                findAllLemmas(w);
                if (w.lemmas.length == 0) {
                    throw new RuntimeException(ServerError.REQUIEST_LEMMA_NOT_FOUND);
                }
            }
        }
        try {
            WordsDetailsChecks.reset();
            SearchResults[] result = new SearchResults[list.length];
            for (int i = 0; i < list.length; i++) {
                Document doc = getApp().processKorpus.getSentence(list[i]);
                result[i] = new SearchResults();
                result[i].docId = list[i];
                result[i].doc = restoreTextInfo(doc);
                result[i].text = restoreText(doc);
                // mark result words
                WordsDetailsChecks.isAllowed(params.wordsOrder, params.words, result[i].text);
            }
            return result;
        } catch (Exception ex) {
            LOGGER.info("<< Result error: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    protected Paragraph restoreText(Document doc) throws Exception {
        LuceneFilter process = getApp().processKorpus;

        byte[] xml = process.getXML(doc);

        Paragraph p = new BinaryParagraphReader(xml).read();
        for (int i = 0; i < p.sentences.length; i++) {
            for (int j = 0; j < p.sentences[i].words.length; j++) {
                p.sentences[i].words[j] = new WordResult(p.sentences[i].words[j]);
            }
        }

        return p;
    }

    protected TextInfo restoreTextInfo(Document doc) throws Exception {
        int textID = getApp().processKorpus.getTextID(doc);
        return getApp().textInfos.get(textID);
    }

    private void findAllLemmas(WordRequest w) {
        Set<String> result = new TreeSet<>();
        Paradigm[] ps = getApp().grFinder.getParadigms(w.word);
        // user can enter lemma of variant, but we need to find paradigm
        for (Paradigm p : ps) {
            for (Variant v : p.getVariant()) {
                if (BelarusianWordNormalizer.equals(v.getLemma(), w.word)) {
                    result.add(p.getLemma());
                    break;
                }
            }
        }
        w.lemmas = result.stream().toArray(String[]::new);
    }
}
