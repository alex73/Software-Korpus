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
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.alex73.korpus.client.SearchService;
import org.alex73.korpus.server.engine.LuceneDriver;
import org.alex73.korpus.shared.ResultSentence;
import org.alex73.korpus.shared.SearchChecks;
import org.alex73.korpus.shared.SearchParams;
import org.alex73.korpus.utils.WordNormalizer;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;

import alex73.corpus.paradigm.S;
import alex73.corpus.paradigm.TEI;
import alex73.corpus.paradigm.W;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * Service for search by corpus documents.
 */
public class SearchServiceImpl extends RemoteServiceServlet implements SearchService {

    protected static JAXBContext CONTEXT;

    static final Logger LOGGER = LogManager.getLogger(SearchServiceImpl.class);

    LuceneDriver lucene;

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
            lucene = new LuceneDriver(new File("Korpus-cache/"), false);
            GrammarDBLite.initializeFromDir(new File("GrammarDB"));
        } catch (Throwable ex) {
            LOGGER.error("startup", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("shutdown");
        try {
            lucene.shutdown();
        } catch (Exception ex) {
            LOGGER.error("shutdown", ex);
        }
        super.destroy();
    }

    @Override
    protected void doUnexpectedFailure(Throwable e) {
        LOGGER.error("UnexpectedFailure", e);
        super.doUnexpectedFailure(e);
    }

    @Override
    public InitialData getInitialData() throws Exception {  
        InitialData result=new InitialData();
        Properties props = new Properties();
        InputStream in = new FileInputStream("Korpus-cache/stat.properties");
        try {
            props.load(in);
        } finally {
            in.close();
        }
        result.authors =Arrays.asList(props.getProperty("authors").split(";"));
        return result;
    }

    @Override
    public SearchResult search(final SearchParams params, LatestMark latest) {
        LOGGER.info(">> Request from " + getThreadLocalRequest().getRemoteAddr());
        if (params.isTooSimple()) {
            LOGGER.info("<< Request too simple");
            throw new RuntimeException(ServerError.REQUIEST_TOO_SIMPLE);
        }
        BooleanQuery query = new BooleanQuery();
        // author
        if (StringUtils.isNotEmpty(params.text.author)) {
            Query q = new TermQuery(new Term(lucene.fieldSentenceTextAuthor.name(), params.text.author));
            query.add(q, BooleanClause.Occur.MUST);
        }
        // style/genre
        if (!params.text.stylegenres.isEmpty()) {
            BooleanQuery q = new BooleanQuery();
            for (String sg : params.text.stylegenres) {
                q.add(new TermQuery(new Term(lucene.fieldSentenceTextStyleGenre.name(), sg)),
                        BooleanClause.Occur.SHOULD);
            }
            q.setMinimumNumberShouldMatch(1);
            query.add(q, BooleanClause.Occur.MUST);
        }
        // written year
        if (params.text.yearWrittenFrom != null || params.text.yearWrittenTo != null) {
            int yFrom = params.text.yearWrittenFrom != null ? params.text.yearWrittenFrom : 1;
            int yTo = params.text.yearWrittenTo != null ? params.text.yearWrittenTo : 9999;
            Query q = NumericRangeQuery.newIntRange(lucene.fieldSentenceTextWrittenYear.name(), yFrom, yTo, true, true);
            query.add(q, BooleanClause.Occur.MUST);
        }
        // published year
        if (params.text.yearPublishedFrom != null || params.text.yearPublishedTo != null) {
            int yFrom = params.text.yearPublishedFrom != null ? params.text.yearPublishedFrom : 1;
            int yTo = params.text.yearPublishedTo != null ? params.text.yearPublishedTo : 9999;
            Query q = NumericRangeQuery.newIntRange(lucene.fieldSentenceTextPublishedYear.name(), yFrom, yTo, true,
                    true);
            query.add(q, BooleanClause.Occur.MUST);
        }
        for (SearchParams.Word w : params.words) {
            w.word = WordNormalizer.normalize(w.word);
            if (w.word.length() > 0) {
                Query wq;
                if (w.allForms) {
                    w.lemmas = findAllLemmas(w.word);
                    if (w.lemmas.isEmpty()) {
                        throw new RuntimeException(ServerError.REQUIEST_LEMMA_NOT_FOUND);
                    }
                    BooleanQuery qLemmas = new BooleanQuery();
                    for (String lemma : w.lemmas) {
                        qLemmas.add(new TermQuery(new Term(lucene.fieldSentenceLemmas.name(), lemma)),
                                BooleanClause.Occur.SHOULD);
                    }
                    wq = qLemmas;
                } else {
                    wq = new TermQuery(new Term(lucene.fieldSentenceValues.name(), w.word));
                }

                query.add(wq, BooleanClause.Occur.MUST);
            }
            if (w.grammar != null) {
                Query wq = new RegexpQuery(new Term(lucene.fieldSentenceDBGrammarTags.name(), w.grammar));
                query.add(wq, BooleanClause.Occur.MUST);
            }
        }

        try {        
            ScoreDoc latestDoc;
            if (latest != null) {
                latestDoc = new ScoreDoc(latest.doc, latest.score, latest.shardIndex);
            } else {
                latestDoc = null;
            }
            ScoreDoc[] found = lucene.search(query, latestDoc, new LuceneDriver.DocFilter() {
                public boolean isDocAllowed(int docID) {
                    try {
                        ResultSentence doc = getSentence(docID);
                        return SearchChecks.isFoundDoc(params, doc);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
            SearchResult result = new SearchResult();
            result.params = params;
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
            LOGGER.error("",ex);
            throw new RuntimeException(ex);
        }
    }

    List<String> findAllLemmas(String word) {
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

    @Override
    public ResultSentence[] getSentences(int[] list) {
        try {
            ResultSentence[] result = new ResultSentence[list.length];
            for (int i = 0; i < list.length; i++) {
                result[i] = getSentence(list[i]);
            }
            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    protected ResultSentence getSentence(int sentenceDocId) throws Exception {
        Document sentenceDoc = lucene.getSentence(sentenceDocId);
        byte[] xml = sentenceDoc.getField(lucene.fieldSentenceXML.name()).binaryValue().bytes;

        Unmarshaller unm = CONTEXT.createUnmarshaller();
        S sentence = (S) unm.unmarshal(new ByteArrayInputStream(xml));

        for (int j = 0; j < sentence.getWOrTag().size(); j++) {
            if (!(sentence.getWOrTag().get(j) instanceof W)) {
                sentence.getWOrTag().remove(j);
                j--;
            }
        }

        ResultSentence result = new ResultSentence();
        result.words = new ResultSentence.Word[sentence.getWOrTag().size()];
        for (int j = 0; j < result.words.length; j++) {
            W w = (W) sentence.getWOrTag().get(j);
            ResultSentence.Word rsw = new ResultSentence.Word();
            rsw.value = w.getValue();
            rsw.lemma = w.getLemma();
            rsw.cat = w.getCat();
            result.words[j] = rsw;
        }

        Field fieldTextId = (Field) sentenceDoc.getField(lucene.fieldSentenceTextID.name());
        int textId = fieldTextId.numericValue().intValue();

        result.doc = lucene.getTextInfo(textId);
        
        return result;
    }
}
