/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2015 Aleś Bułojčyk (alex73mail@gmail.com)
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alex73.korpus.base.OtherInfo;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.server.engine.LuceneDriverRead;
import org.alex73.korpus.server.engine.LuceneDriverRead.DocFilter;
import org.alex73.korpus.shared.dto.StandardTextRequest;
import org.alex73.korpus.shared.dto.UnprocessedTextRequest;
import org.alex73.korpus.shared.dto.WordRequest;
import org.alex73.korpus.utils.WordNormalizer;
import org.apache.commons.lang.StringUtils;
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
import org.apache.lucene.search.WildcardQuery;

public class LuceneFilter {
    LuceneDriverRead lucene;

    public LuceneFilter(String dir) throws Exception {
        lucene = new LuceneDriverRead(dir);
    }

    public void close() throws Exception {
        lucene.shutdown();
    }

    public void addKorpusTextFilter(BooleanQuery query, StandardTextRequest filter) {
        // author
        if (StringUtils.isNotEmpty(filter.author)) {
            Query q = new TermQuery(new Term(lucene.fieldSentenceTextAuthor.name(), filter.author));
            query.add(q, BooleanClause.Occur.MUST);
        }
        // style/genre
        if (!filter.stylegenres.isEmpty()) {
            BooleanQuery q = new BooleanQuery();
            for (String sg : filter.stylegenres) {
                q.add(new TermQuery(new Term(lucene.fieldSentenceTextStyleGenre.name(), sg)),
                        BooleanClause.Occur.SHOULD);
            }
            q.setMinimumNumberShouldMatch(1);
            query.add(q, BooleanClause.Occur.MUST);
        }
        // written year
        if (filter.yearWrittenFrom != null || filter.yearWrittenTo != null) {
            int yFrom = filter.yearWrittenFrom != null ? filter.yearWrittenFrom : 1;
            int yTo = filter.yearWrittenTo != null ? filter.yearWrittenTo : 9999;
            Query q = NumericRangeQuery.newIntRange(lucene.fieldSentenceTextWrittenYear.name(), yFrom, yTo,
                    true, true);
            query.add(q, BooleanClause.Occur.MUST);
        }
        // published year
        if (filter.yearPublishedFrom != null || filter.yearPublishedTo != null) {
            int yFrom = filter.yearPublishedFrom != null ? filter.yearPublishedFrom : 1;
            int yTo = filter.yearPublishedTo != null ? filter.yearPublishedTo : 9999;
            Query q = NumericRangeQuery.newIntRange(lucene.fieldSentenceTextPublishedYear.name(), yFrom, yTo,
                    true, true);
            query.add(q, BooleanClause.Occur.MUST);
        }
    }

    public void addOtherTextFilter(BooleanQuery query, UnprocessedTextRequest filter) {
        // volume
        if (StringUtils.isNotEmpty(filter.volume)) {
            Query q = new TermQuery(new Term(lucene.fieldSentenceTextVolume.name(), filter.volume));
            query.add(q, BooleanClause.Occur.MUST);
        }
    }

    public void addWordFilter(BooleanQuery query, WordRequest w) {
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
                    qLemmas.add(new TermQuery(getLemmaTerm(lemma)), BooleanClause.Occur.SHOULD);
                }
                wq = qLemmas;
            } else {
                if (w.isWildcardWord()) {
                    // has wildcard
                    wq = new WildcardQuery(getValueTerm(w.word));
                } else {
                    // simple word
                    wq = new TermQuery(getValueTerm(w.word));
                }
            }
            query.add(wq, BooleanClause.Occur.MUST);
        }
        if (w.grammar != null) {
            Query wq = new RegexpQuery(getGrammarTerm(w.grammar));
            query.add(wq, BooleanClause.Occur.MUST);
        }
    }

    private Term getLemmaTerm(String lemma) {
        return new Term(lucene.fieldSentenceLemmas.name(), lemma);
    }

    private Term getValueTerm(String value) {
        return new Term(lucene.fieldSentenceValues.name(), value);
    }

    private Term getGrammarTerm(String grammar) {
        return new Term(lucene.fieldSentenceDBGrammarTags.name(), grammar);
    }

    public ScoreDoc[] search(Query query, ScoreDoc latest, int maxResults, DocFilter filter) throws Exception {
        return lucene.search(query, latest, maxResults, filter);
    }

    public Document getSentence(int docID) throws Exception {
        return lucene.getSentence(docID);
    }

    public byte[] getXML(Document doc) {
        return doc.getField(lucene.fieldSentenceXML.name()).binaryValue().bytes;
    }

    public TextInfo getKorpusTextInfo(Document doc) throws Exception {
        Field fieldTextId = (Field) doc.getField(lucene.fieldSentenceTextID.name());
        int textId = fieldTextId.numericValue().intValue();

        return lucene.getTextInfo(textId);
    }

    public OtherInfo getOtherInfo(Document doc) throws Exception {
        OtherInfo info = new OtherInfo();
        info.textURL = doc.getField(lucene.fieldSentenceTextURL.name()).stringValue();
        return info;
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
