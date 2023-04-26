package org.alex73.korpus.server;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.server.data.ChainRequest;
import org.alex73.korpus.server.data.ClusterParams;
import org.alex73.korpus.server.data.ClusterResults;
import org.alex73.korpus.server.data.FreqSpisResult;
import org.alex73.korpus.server.data.InitialData;
import org.alex73.korpus.server.data.LatestMark;
import org.alex73.korpus.server.data.SearchParams;
import org.alex73.korpus.server.data.SearchResult;
import org.alex73.korpus.server.data.SearchResults;
import org.alex73.korpus.server.data.SearchTotalResult;
import org.alex73.korpus.server.data.WordRequest;
import org.alex73.korpus.server.data.WordRequest.WordMode;
import org.alex73.korpus.server.data.WordResult;
import org.alex73.korpus.server.engine.LuceneDriverRead;
import org.alex73.korpus.server.text.BinaryParagraphReader;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.utils.KorpusFileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;

@Path("/korpus")
public class SearchServiceImpl {
    private final static Logger LOGGER = Logger.getLogger(SearchServiceImpl.class.getName());
    private static final Collator BE = Collator.getInstance(new Locale("be"));

    @Context
    HttpServletRequest request;

    private ApplicationKorpus getApp() {
        return ApplicationKorpus.instance;
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

    @Path("freq")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<FreqSpisResult> getFrequences(@QueryParam("subcorpus") String subcorpus) throws Exception {
        LOGGER.info("getFrequences from " + request.getRemoteAddr());
        try {
            List<String> data = KorpusFileUtils.readZip(Paths.get(getApp().korpusCache).resolve("stat-freq.zip"), "forms/freq." + subcorpus + ".tab");
            return data.stream().map(s -> {
                int p = s.indexOf('=');
                FreqSpisResult r = new FreqSpisResult();
                r.w = s.substring(0, p);
                r.c = Integer.parseInt(s.substring(p + 1));
                return r;
            }).sorted((a, b) -> BE.compare(a.w, b.w)).collect(Collectors.toList());
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "getFrequences", ex);
            throw new Exception("Data not found for subcorpus '" + subcorpus + "'");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "getFrequences", ex);
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
        try {
            ILanguage lang = LanguageFactory.get(rq.params.lang);
            SearchParams params = rq.params;
            LatestMark latest = rq.latest;
            checkEnoughComplex(params);
            params.chains.forEach(ch -> ch.words.forEach(w -> findAllLemmas(lang, w)));

            BooleanQuery.Builder query = new BooleanQuery.Builder();
            LuceneFilter process = getApp().processKorpus;
            process.addKorpusTextFilter(rq.params.lang, query, params.textStandard);
            params.chains.forEach(ch -> ch.words.forEach(w -> process.addWordFilter(rq.params.lang, query, w)));

            if (latest == null) {
                latest = new LatestMark();
            }
            List<Integer> found = process.search(query.build(), latest, Settings.KORPUS_SEARCH_RESULT_PAGE, filterFoundDocumentsByChains(rq.params, lang));
            SearchResult result = new SearchResult();
            result.hasMore = found.size() >= Settings.KORPUS_SEARCH_RESULT_PAGE;
            result.foundIDs = new int[found.size()];
            for (int i = 0; i < result.foundIDs.length; i++) {
                result.foundIDs[i] = found.get(i);
            }
            result.latest = latest;
            LOGGER.info("<< Result: found: " + result.foundIDs.length + " hasMore:" + result.hasMore);
            return result;
        } catch (ServerError ex) {
            SearchResult result = new SearchResult();
            result.error = ex.getMessage();
            return result;
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "<< Result error", ex);
            throw ServerError.internalError();
        }
    }

    @Path("searchTotalCount")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SearchTotalResult searchTotalCount(SearchRequest rq) throws Exception {
        LOGGER.info(">> Request total count from " + request.getRemoteAddr());
        try {
            ILanguage lang = LanguageFactory.get(rq.params.lang);
            SearchParams params = rq.params;
            checkEnoughComplex(params);
            params.chains.forEach(ch -> ch.words.forEach(w -> findAllLemmas(lang, w)));

            BooleanQuery.Builder query = new BooleanQuery.Builder();
            LuceneFilter process = getApp().processKorpus;
            process.addKorpusTextFilter(rq.params.lang, query, params.textStandard);
            params.chains.forEach(ch -> ch.words.forEach(w -> process.addWordFilter(rq.params.lang, query, w)));

            List<Integer> found = process.search(query.build(), new LatestMark(), Settings.KORPUS_SEARCH_TOTAL_MAX_COUNT,
                    filterFoundDocumentsByChains(rq.params, lang));
            SearchTotalResult result = new SearchTotalResult();
            result.totalCount = found.size();
            LOGGER.info("<< Result: found: " + result.totalCount);
            return result;
        } catch (ServerError ex) {
            SearchTotalResult result = new SearchTotalResult();
            result.error = ex.getMessage();
            return result;
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "<< Result error", ex);
            throw ServerError.internalError();
        }
    }

    @Path("cluster")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ClusterResults calculateClusters(ClusterParams params) throws Exception {
        LOGGER.info(">> Request clusters from " + request.getRemoteAddr());
        try {
            ILanguage lang = LanguageFactory.get(params.lang);
            if (WordsDetailsChecks.isTooSimpleWord(params.word)) {
                LOGGER.info("<< Request too simple");
                throw ServerError.tooSimple();
            }
            findAllLemmas(lang, params.word);

            LuceneFilter corpusFilter = getApp().processKorpus;
            ClusterResults res = new ClusterServiceImpl(this).calc(params, corpusFilter);
            LOGGER.info("<< Result clusters");
            return res;
        } catch (ServerError ex) {
            throw ex;
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "<< Result error", ex);
            throw ServerError.internalError();
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
        ILanguage lang = LanguageFactory.get(rq.params.lang);
        SearchParams params = rq.params;
        int[] list = rq.list;
        params.chains.forEach(ch -> ch.words.forEach(w -> findAllLemmas(lang, w)));
        WordsDetailsChecks checks = new WordsDetailsChecks(lang, params.chains, params.chainsInParagraph, getApp().grFiller);
        try {
            SearchResults[] result = new SearchResults[list.length];
            for (int i = 0; i < list.length; i++) {
                Document doc = getApp().processKorpus.getSentence(list[i]);
                result[i] = new SearchResults();
                result[i].docId = list[i];
                result[i].doc = restoreTextInfo(doc);
                result[i].text = restoreText(doc);
                // mark result words
                checks.isAllowed(result[i].text);
            }
            return result;
        } catch (ServerError ex) {
            throw ex;
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "<< Result error", ex);
            throw ServerError.internalError();
        }
    }

    protected Paragraph[] restoreText(Document doc) throws Exception {
        LuceneFilter process = getApp().processKorpus;

        byte[] xml = process.getXML(doc);

        Paragraph[] ps = new BinaryParagraphReader(xml).read();
        for (int pi = 0; pi < ps.length; pi++) {
            for (int i = 0; i < ps[pi].sentences.length; i++) {
                for (int j = 0; j < ps[pi].sentences[i].words.length; j++) {
                    ps[pi].sentences[i].words[j] = new WordResult(ps[pi].sentences[i].words[j]);
                }
            }
        }

        return ps;
    }

    protected TextInfo restoreTextInfo(Document doc) throws Exception {
        int textID = getApp().processKorpus.getTextID(doc);
        return getApp().getTextInfo(textID);
    }

    private void findAllLemmas(ILanguage lang, WordRequest w) {
        if (w.mode != WordMode.ALL_FORMS) {
            return;
        }
        Set<String> forms = new TreeSet<>();
        Paradigm[] ps = getApp().grFinder.getParadigms(w.word);
        // user can enter lemma of variant, but we need to find paradigm
        for (Paradigm p : ps) {
            for (Variant v : p.getVariant()) {
                if (lang.getNormalizer().lightNormalized(v.getLemma(), ILanguage.INormalizer.PRESERVE_NONE).equals(w.word)) {
                    p.getVariant().forEach(vv -> vv.getForm().forEach(f -> forms.add(f.getValue())));
                    break;
                }
            }
        }
        forms.remove("");
        if (forms.isEmpty()) {
            throw ServerError.lemmaNotFound(w.word);
        }
        w.forms = forms.toArray(String[]::new);
    }

    private void checkEnoughComplex(SearchParams params) {
        boolean enoughComplex = false;
        for (ChainRequest ch : params.chains) {
            for (WordRequest w : ch.words) {
                if (!WordsDetailsChecks.isTooSimpleWord(w)) {
                    enoughComplex = true;
                    break;
                }
            }
        }
        if (!enoughComplex) {
            LOGGER.info("<< Request too simple");
            throw ServerError.tooSimple();
        }
    }

    private LuceneDriverRead.DocFilter<Integer> filterFoundDocumentsByChains(SearchParams params, ILanguage lang) {
        WordsDetailsChecks checks = new WordsDetailsChecks(lang, params.chains, params.chainsInParagraph, getApp().grFiller);
        return new LuceneDriverRead.DocFilter<>() {

            public Integer processDoc(int docID) {
                try {
                    Document doc = getApp().processKorpus.getSentence(docID);
                    Paragraph[] text = restoreText(doc);
                    return checks.isAllowed(text) ? docID : null;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }
}
