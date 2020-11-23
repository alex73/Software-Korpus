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

import java.util.List;

import org.alex73.korpus.base.OtherInfo;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.server.data.LatestMark;
import org.alex73.korpus.server.data.StandardTextRequest;
import org.alex73.korpus.server.data.UnprocessedTextRequest;
import org.alex73.korpus.server.data.WordRequest;
import org.alex73.korpus.server.engine.LuceneDriverRead;
import org.alex73.korpus.server.engine.LuceneDriverRead.DocFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
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
        if (filter.authors != null) {
            BooleanQuery q = new BooleanQuery();
            for (String a : filter.authors) {
                q.add(new TermQuery(new Term(lucene.fieldSentenceTextAuthor.name(), a)),
                        BooleanClause.Occur.SHOULD);
            }
            q.setMinimumNumberShouldMatch(1);
            query.add(q, BooleanClause.Occur.MUST);
        }
        // style/genre
        if (filter.stylegenres != null) {
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

    @Deprecated
    public void addOtherTextFilter(BooleanQuery query, UnprocessedTextRequest filter) {
        // volume
        if (StringUtils.isNotEmpty(filter.volume)) {
            Query q = new TermQuery(new Term(lucene.fieldSentenceOtherVolume.name(), filter.volume));
            query.add(q, BooleanClause.Occur.MUST);
        }
    }

    public void addWordFilter(BooleanQuery query, WordRequest w) {
        if (w.word.length() > 0) {
            Query wq;
            if (w.allForms) {
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
    
    public void search(Query query, int pageSize, DocFilter<Void> filter) throws Exception {
        lucene.search(query, pageSize, filter);
    }

    public <T> List<T> search(Query query, LatestMark latest, int maxResults, DocFilter<T> filter) throws Exception {
        return lucene.search(query, latest, maxResults, filter);
    }

    public Document getSentence(int docID) throws Exception {
        return lucene.getSentence(docID);
    }

    public byte[] getXML(Document doc) {
        return doc.getField(lucene.fieldSentencePBinary.name()).binaryValue().bytes;
    }

    public TextInfo getKorpusTextInfo(Document doc) throws Exception {
        Field fieldTextId = (Field) doc.getField(lucene.fieldSentenceTextID.name());
        int textId = fieldTextId.numericValue().intValue();

        return lucene.getTextInfo(textId);
    }

    public OtherInfo getOtherInfo(Document doc) throws Exception {
        OtherInfo info = new OtherInfo();
        info.name = doc.getField(lucene.fieldSentenceOtherName.name()).stringValue();
        info.url = doc.getField(lucene.fieldSentenceOtherURL.name()).stringValue();
        info.details = doc.getField(lucene.fieldSentenceOtherDetails.name()).stringValue();
        return info;
    }
}
