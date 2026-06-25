package org.alex73.korpus.server;

import org.alex73.grammardb.structures.Paradigm;
import org.alex73.grammardb.structures.Variant;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.server.data.*;
import org.alex73.korpus.server.data.WordRequest.WordMode;
import org.alex73.korpus.server.engine.LuceneDriverRead;
import org.alex73.korpus.server.text.BinaryParagraphReader;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.utils.KorpusFileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SearchServiceImpl {
    private final static Logger LOGGER = Logger.getLogger(SearchServiceImpl.class.getName());
    private static final Collator BE = Collator.getInstance(Locale.of("be"));

    private static final Map<String, List<FreqSpisResult>> freqsBySubcorpus = new ConcurrentHashMap<>();

    private final ApplicationKorpus app;

    public SearchServiceImpl(ApplicationKorpus app) {
        this.app = app;
    }

    public InitialData getInitialData(String remoteAddr) throws Exception {
        LOGGER.info("getInitialData from " + remoteAddr);
        try {
            return app.searchInitial;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "getInitialData", ex);
            throw ex;
        }
    }

    public List<FreqSpisResult> getFrequences(String subcorpus, String remoteAddr) throws Exception {
        LOGGER.info("getFrequences from " + remoteAddr);

        List<FreqSpisResult> result = freqsBySubcorpus.computeIfAbsent(subcorpus, sc -> {
            try {
                List<String> data = KorpusFileUtils.readZip(Paths.get(app.korpusCachePath).resolve("stat-freq.zip"), "forms/freq." + sc + ".tab");
                return data.stream().map(s -> {
                    int p = s.indexOf('=');
                    FreqSpisResult r = new FreqSpisResult();
                    r.w = s.substring(0, p);
                    r.c = Integer.parseInt(s.substring(p + 1));
                    return r;
                }).sorted((a, b) -> BE.compare(a.w, b.w)).collect(Collectors.toList());
            } catch (FileNotFoundException ex) {
                LOGGER.log(Level.SEVERE, "getFrequences", ex);
                return null;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "getFrequences", ex);
                throw new RuntimeException(ex);
            }
        });

        if (result == null) {
            throw new Exception("Data not found for subcorpus '" + subcorpus + "'");
        }
        return result;
    }

    public static class SearchRequest {
        public SearchParams params;
        public LatestMark latest;
    }

    public SearchResult search(SearchRequest rq, String remoteAddr) throws Exception {
        LOGGER.info(">> Request from " + remoteAddr);
        try {
            ILanguage lang = LanguageFactory.get(rq.params.lang);
            SearchParams params = rq.params;
            LatestMark latest = rq.latest;
            params.chains.forEach(ch -> ch.words.forEach(w -> checkWord(lang, w)));
            checkEnoughComplex(params);
            SearchResult result = new SearchResult();
            params.chains.forEach(ch -> ch.words.forEach(w -> findAllLemmas(lang, w)));

            BooleanQuery.Builder query = new BooleanQuery.Builder();
            LuceneFilter process = app.processKorpus;
            process.addKorpusTextFilter(rq.params.lang, query, params.textStandard);
            params.chains.forEach(ch -> ch.words.forEach(w -> process.addWordFilter(rq.params.lang, query, w)));

            if (latest == null) {
                latest = new LatestMark();
            }
            List<Integer> found = process.search(query.build(), latest, Settings.KORPUS_SEARCH_RESULT_PAGE, filterFoundDocumentsByChains(rq.params, lang));
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

    public SearchTotalResult searchTotalCount(SearchRequest rq, String remoteAddr) throws Exception {
        LOGGER.info(">> Request total count from " + remoteAddr);
        try {
            ILanguage lang = LanguageFactory.get(rq.params.lang);
            SearchParams params = rq.params;
            params.chains.forEach(ch -> ch.words.forEach(w -> checkWord(lang, w)));
            checkEnoughComplex(params);
            params.chains.forEach(ch -> ch.words.forEach(w -> findAllLemmas(lang, w)));

            BooleanQuery.Builder query = new BooleanQuery.Builder();
            LuceneFilter process = app.processKorpus;
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

    public ClusterResults calculateClusters(ClusterParams params, String remoteAddr) throws Exception {
        LOGGER.info(">> Request clusters from " + remoteAddr);
        try {
            ILanguage lang = LanguageFactory.get(params.lang);
            checkWord(lang, params.word);
            if (WordsDetailsChecks.isTooSimpleWord(params.word)) {
                LOGGER.info("<< Request too simple");
                throw ServerError.tooSimple();
            }
            findAllLemmas(lang, params.word);

            LuceneFilter corpusFilter = app.processKorpus;
            ClusterResults res = new ClusterServiceImpl(this, app).calc(params, corpusFilter);
            LOGGER.info("<< Result clusters");
            return res;
        } catch (ServerError ex) {
            ClusterResults result = new ClusterResults();
            result.error = ex.getMessage();
            return result;
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "<< Result error", ex);
            throw ServerError.internalError();
        }
    }

    public static class SentencesRequest {
        public SearchParams params;
        public int[] list;
    }

    public SearchResults[] getSentences(SentencesRequest rq) throws Exception {
        ILanguage lang = LanguageFactory.get(rq.params.lang);
        SearchParams params = rq.params;
        int[] list = rq.list;
        params.chains.forEach(ch -> ch.words.forEach(w -> findAllLemmas(lang, w)));
        WordsDetailsChecks checks = WordsDetailsChecks.createForSearch(app.isAuthorsBlacklisted, lang, params.chains, params.chainsInParagraph, app.grFiller);
        try {
            SearchResults[] result = new SearchResults[list.length];
            for (int i = 0; i < list.length; i++) {
                Document doc = app.processKorpus.getSentence(list[i]);
                result[i] = new SearchResults();
                result[i].docId = list[i];
                result[i].doc = restoreTextInfo(doc);
                result[i].text = restoreText(doc);
                // mark result words
                checks.isAllowed(result[i].doc, result[i].text);
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
        LuceneFilter process = app.processKorpus;

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
        int textID = app.processKorpus.getTextID(doc);
        return app.infos.textInfos.get(textID);
    }

    private void findAllLemmas(ILanguage lang, WordRequest w) {
        if (w.mode != WordMode.ALL_FORMS) {
            return;
        }
        w.word = lang.getNormalizer().lightNormalized(w.word, ILanguage.INormalizer.PRESERVE_NONE);
        Set<String> forms = new TreeSet<>();
        Paradigm[] ps = app.grFinder.getParadigms(w.word);
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

    private void checkWord(ILanguage lang, WordRequest w) throws ServerError {
        if (lang.getDbTags() == null) {
            // non-Belarusian language
            if (w.mode == WordMode.ALL_FORMS || w.grammar != null) {
                LOGGER.info("<< Grammar request for non-Belarusian language");
                throw ServerError.rusForms();
            }
            if (w.variants) {
                LOGGER.info("<< Grammar request for non-Belarusian language");
                throw ServerError.rusVariants();
            }
        }
        if ((w.word == null || w.word.isBlank()) && w.mode != WordMode.GRAMMAR) {
            if (w.grammar != null) {
                throw ServerError.noWordButGrammar();
            } else {
                throw ServerError.noWord();
            }
        }
    }

    private LuceneDriverRead.DocFilter<Integer> filterFoundDocumentsByChains(SearchParams params, ILanguage lang) {
        WordsDetailsChecks checks = WordsDetailsChecks.createForSearch(app.isAuthorsBlacklisted,lang, params.chains, params.chainsInParagraph, app.grFiller);
        return new LuceneDriverRead.DocFilter<>() {

            public Integer processDoc(int docID) {
                try {
                    Document doc = app.processKorpus.getSentence(docID);
                    TextInfo textInfo = app.infos.textInfos.get(app.processKorpus.getTextID(doc));
                    Paragraph[] text = restoreText(doc);
                    return checks.isAllowed(textInfo, text) ? docID : null;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }
}
