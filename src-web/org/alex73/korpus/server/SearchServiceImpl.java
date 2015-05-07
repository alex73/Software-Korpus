/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2013 Aleś Bułojčyk (alex73mail@gmail.com)
               Home page: https://sourceforge.net/projects/korpus/

 This file is part of Korpus.

 Korpus is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Korpus is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.alex73.korpus.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.alex73.korpus.client.SearchService;
import org.alex73.korpus.server.engine.LuceneDriverRead;
import org.alex73.korpus.shared.dto.ClusterParams;
import org.alex73.korpus.shared.dto.ClusterResults;
import org.alex73.korpus.shared.dto.CorpusType;
import org.alex73.korpus.shared.dto.ResultText;
import org.alex73.korpus.shared.dto.SearchParams;
import org.alex73.korpus.shared.dto.SearchResults;
import org.alex73.korpus.shared.dto.WordRequest;
import org.alex73.korpus.shared.dto.WordResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ScoreDoc;

import alex73.corpus.paradigm.P;
import alex73.corpus.paradigm.S;
import alex73.corpus.paradigm.TEI;
import alex73.corpus.paradigm.W;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * Service for search by corpus documents.
 */
@SuppressWarnings("serial")
public class SearchServiceImpl extends RemoteServiceServlet implements SearchService {

    protected static JAXBContext CONTEXT;

    static final Logger LOGGER = LogManager.getLogger(SearchServiceImpl.class);

    public static String dirPrefix = System.getProperty("KORPUS_DIR");
    LuceneFilter processKorpus;
    LuceneFilter processOther;

    static {
        try {
            CONTEXT = JAXBContext.newInstance(TEI.class.getPackage().getName());
        } catch (Exception ex) {
            LOGGER.error("JAXB initialization", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public SearchServiceImpl() {
        LOGGER.info("startup");
        try {
            GrammarDBLite.initializeFromDir(new File(dirPrefix + "/GrammarDB/"));
            processKorpus = new LuceneFilter(dirPrefix + "/Korpus-cache/");
            processOther = new LuceneFilter(dirPrefix + "/Other-cache/");
        } catch (Throwable ex) {
            LOGGER.error("startup", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("shutdown");
        try {
            processKorpus.close();
            processOther.close();
        } catch (Exception ex) {
            LOGGER.error("shutdown", ex);
        }
        super.destroy();
    }

    protected LuceneFilter getProcess(CorpusType corpusType) {
        if (corpusType == CorpusType.STANDARD) {
            return processKorpus;
        } else {
            return processOther;
        }
    }

    @Override
    protected void doUnexpectedFailure(Throwable e) {
        LOGGER.error("UnexpectedFailure", e);
        super.doUnexpectedFailure(e);
    }

    @Override
    public InitialData getInitialData() throws Exception {
        LOGGER.info(">> getInitialData");
        try {
            InitialData result = new InitialData();

            Properties props = new Properties();
            try (InputStream in = new FileInputStream(dirPrefix + "/Korpus-cache/stat.properties")) {
                props.load(in);
            }
            result.authors = Arrays.asList(props.getProperty("authors").split(";"));
            result.statKorpus = new HashMap<>();
            for (String k : (Set<String>) (Set) props.keySet()) {
                if (k.startsWith("texts") || k.startsWith("words")) {
                    result.statKorpus.put(k, Integer.parseInt(props.getProperty(k)));
                }
            }

            props = new Properties();
            try (InputStream in = new FileInputStream(dirPrefix + "/Other-cache/stat.properties")) {
                props.load(in);
            }
            result.volumes = Arrays.asList(props.getProperty("volumes").split(";"));
            result.statOther = new HashMap<>();
            for (String k : (Set<String>) (Set) props.keySet()) {
                if (k.startsWith("texts") || k.startsWith("words")) {
                    result.statOther.put(k, Integer.parseInt(props.getProperty(k)));
                }
            }

            LOGGER.info("<< getInitialData");
            return result;
        } catch (Exception ex) {
            LOGGER.error("getInitialData", ex);
            throw ex;
        }
    }

    @Override
    public SearchResult search(final SearchParams params, LatestMark latest) throws Exception {
        LOGGER.info(">> Request from " + getThreadLocalRequest().getRemoteAddr());
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
            BooleanQuery query = new BooleanQuery();
            LuceneFilter process = getProcess(params.corpusType);
            if (params.corpusType == CorpusType.STANDARD) {
                process.addKorpusTextFilter(query, params.textStandard);
            } else {
                process.addOtherTextFilter(query, params.textUnprocessed);
            }

            for (WordRequest w : params.words) {
                process.addWordFilter(query, w);
            }

            ScoreDoc latestDoc;
            if (latest != null) {
                latestDoc = new ScoreDoc(latest.doc, latest.score, latest.shardIndex);
            } else {
                latestDoc = null;
            }
            ScoreDoc[] found = process.search(query, latestDoc, Settings.KORPUS_SEARCH_RESULT_PAGE + 1,
                    new LuceneDriverRead.DocFilter() {
                        public boolean isDocAllowed(int docID) {
                            try {
                                Document doc = getProcess(params.corpusType).getSentence(docID);
                                ResultText text = restoreText(params.corpusType, doc);
                                return WordsDetailsChecks.isAllowed(params.wordsOrder, params.words, text);
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    });
            SearchResult result = new SearchResult();
            if (found.length > Settings.KORPUS_SEARCH_RESULT_PAGE) {
                result.hasMore = true;
                result.foundIDs = new int[Settings.KORPUS_SEARCH_RESULT_PAGE];
            } else {
                result.hasMore = false;
                result.foundIDs = new int[found.length];
            }
            for (int i = 0; i < result.foundIDs.length; i++) {
                result.foundIDs[i] = found[i].doc;
            }
            if (result.hasMore) {
                ScoreDoc f = found[result.foundIDs.length - 1];
                result.latest = new LatestMark();
                result.latest.doc = f.doc;
                result.latest.score = f.score;
                result.latest.shardIndex = f.shardIndex;
            }
            LOGGER.info("<< Result: found: " + result.foundIDs.length + " hasMore:" + result.hasMore);
            return result;
        } catch (Throwable ex) {
            LOGGER.error("search", ex);
            throw ex;
        }
    }

    @Override
    public ClusterResults calculateClusters(ClusterParams params) throws Exception {
        LOGGER.info(">> Request clusters from " + getThreadLocalRequest().getRemoteAddr());
        try {
            WordsDetailsChecks.reset();

            if (WordsDetailsChecks.isTooSimpleWord(params.word)) {
                LOGGER.info("<< Request too simple");
                throw new RuntimeException(ServerError.REQUIEST_TOO_SIMPLE);
            }

            ClusterResults res = new ClusterServiceImpl(this).calc(params);
            LOGGER.info("<< Result clusters");
            return res;
        } catch (Throwable ex) {
            LOGGER.error("clusters", ex);
            throw ex;
        }
    }

    @Override
    public SearchResults[] getSentences(SearchParams params, int[] list) throws Exception {
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
            LOGGER.error("getSentences", ex);
            throw ex;
        }
    }

    protected void restoreTextInfo(SearchParams params, Document doc, SearchResults result) throws Exception {
        if (params.corpusType == CorpusType.STANDARD) {
            result.doc = processKorpus.getKorpusTextInfo(doc);
        } else {
            result.docOther = processOther.getOtherInfo(doc);
        }
    }

    protected ResultText restoreText(CorpusType corpusType, Document doc) throws Exception {
        LuceneFilter process;
        if (corpusType == CorpusType.STANDARD) {
            process = processKorpus;
        } else {
            process = processOther;
        }

        byte[] xml = process.getXML(doc);

        Unmarshaller unm = CONTEXT.createUnmarshaller();
        P paragraph;
        if (Settings.GZIP_TEXT_XML) {
            paragraph = (P) unm.unmarshal(new GZIPInputStream(new ByteArrayInputStream(xml)));
        } else {
            paragraph = (P) unm.unmarshal(new ByteArrayInputStream(xml));
        }

        List<WordResult[]> sentences = new ArrayList<>();

        for (int i = 0; i < paragraph.getSOrTag().size(); i++) {
            if (!(paragraph.getSOrTag().get(i) instanceof S)) {
                continue;
            }
            S sentence = (S) paragraph.getSOrTag().get(i);
            List<WordResult> words = new ArrayList<>();
            for (int j = 0; j < sentence.getWOrTag().size(); j++) {
                if (!(sentence.getWOrTag().get(j) instanceof W)) {
                    continue;
                }
                W w = (W) sentence.getWOrTag().get(j);
                WordResult rsw = new WordResult();
                rsw.value = w.getValue();
                rsw.lemma = w.getLemma();
                rsw.cat = w.getCat();
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
}
