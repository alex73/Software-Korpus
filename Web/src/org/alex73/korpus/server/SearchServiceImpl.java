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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.JAXBContext;

import org.alex73.korpus.client.SearchService;
import org.alex73.korpus.server.engine.LuceneDriverRead;
import org.alex73.korpus.server.text.BinaryParagraphReader;
import org.alex73.korpus.shared.dto.ClusterParams;
import org.alex73.korpus.shared.dto.ClusterResults;
import org.alex73.korpus.shared.dto.CorpusType;
import org.alex73.korpus.shared.dto.LatestMark;
import org.alex73.korpus.shared.dto.ResultText;
import org.alex73.korpus.shared.dto.SearchParams;
import org.alex73.korpus.shared.dto.SearchResults;
import org.alex73.korpus.shared.dto.WordRequest;
import org.alex73.korpus.shared.dto.WordResult;
import org.alex73.korpus.text.xml.O;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.S;
import org.alex73.korpus.text.xml.Se;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.Z;
import org.alex73.korpus.utils.WordNormalizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;

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
            if (params.corpusType == CorpusType.STANDARD) {
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
            if (params.word.allForms) {
                params.word.lemmas = findAllLemmas(params.word.word);
                if (params.word.lemmas.isEmpty()) {
                    throw new RuntimeException(ServerError.REQUIEST_LEMMA_NOT_FOUND);
                }
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

        P paragraph = new BinaryParagraphReader(xml).read();

        List<WordResult[]> sentences = new ArrayList<>();

        for (int i = 0; i < paragraph.getSe().size(); i++) {
            Se sentence = paragraph.getSe().get(i);
            List<WordResult> words = new ArrayList<>();
            for (int j = 0; j < sentence.getWOrSOrZ().size(); j++) {
                Object o = sentence.getWOrSOrZ().get(j);
                WordResult rsw = new WordResult();
                if (o instanceof W) {
                    rsw.value = ((W) o).getValue();
                    rsw.lemma = ((W) o).getLemma();
                    rsw.cat = ((W) o).getCat();
                    rsw.isWord = true;
                } else if (o instanceof S) {
                    rsw.value = ((S) o).getChar();
                } else if (o instanceof Z) {
                    rsw.value = ((Z) o).getValue();
                } else if (o instanceof O) {
                    rsw.value = ((O) o).getValue();
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
        Set<String> result = new HashSet<>();
        for (LiteParadigm p : GrammarDBLite.getInstance().getAllParadigms()) {
            for (LiteForm f : p.forms) {
                if (word.equals(WordNormalizer.normalize(f.value))) {
                    result.add(p.lemma);
                    break;
                }
            }
        }
        return new ArrayList<>(result);
    }
}