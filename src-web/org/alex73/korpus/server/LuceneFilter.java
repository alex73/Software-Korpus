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

import org.alex73.korpus.base.OtherInfo;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.server.engine.LuceneDriverRead;
import org.alex73.korpus.server.engine.LuceneDriverRead.DocFilter;
import org.alex73.korpus.shared.dto.SearchParams;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;

public class LuceneFilter {
    LuceneDriverRead lucene;

    public LuceneFilter(String dir) throws Exception {
        lucene = new LuceneDriverRead(dir);
    }

    public void close() throws Exception {
        lucene.shutdown();
    }

    public void addKorpusTextFilter(BooleanQuery query, SearchParams params) {
        // author
        if (StringUtils.isNotEmpty(params.textStandard.author)) {
            Query q = new TermQuery(new Term(lucene.fieldSentenceTextAuthor.name(),
                    params.textStandard.author));
            query.add(q, BooleanClause.Occur.MUST);
        }
        // style/genre
        if (!params.textStandard.stylegenres.isEmpty()) {
            BooleanQuery q = new BooleanQuery();
            for (String sg : params.textStandard.stylegenres) {
                q.add(new TermQuery(new Term(lucene.fieldSentenceTextStyleGenre.name(), sg)),
                        BooleanClause.Occur.SHOULD);
            }
            q.setMinimumNumberShouldMatch(1);
            query.add(q, BooleanClause.Occur.MUST);
        }
        // written year
        if (params.textStandard.yearWrittenFrom != null || params.textStandard.yearWrittenTo != null) {
            int yFrom = params.textStandard.yearWrittenFrom != null ? params.textStandard.yearWrittenFrom : 1;
            int yTo = params.textStandard.yearWrittenTo != null ? params.textStandard.yearWrittenTo : 9999;
            Query q = NumericRangeQuery.newIntRange(lucene.fieldSentenceTextWrittenYear.name(), yFrom, yTo,
                    true, true);
            query.add(q, BooleanClause.Occur.MUST);
        }
        // published year
        if (params.textStandard.yearPublishedFrom != null || params.textStandard.yearPublishedTo != null) {
            int yFrom = params.textStandard.yearPublishedFrom != null ? params.textStandard.yearPublishedFrom
                    : 1;
            int yTo = params.textStandard.yearPublishedTo != null ? params.textStandard.yearPublishedTo
                    : 9999;
            Query q = NumericRangeQuery.newIntRange(lucene.fieldSentenceTextPublishedYear.name(), yFrom, yTo,
                    true, true);
            query.add(q, BooleanClause.Occur.MUST);
        }
    }

    public void addOtherTextFilter(BooleanQuery query, SearchParams params) {
        // volume
        if (StringUtils.isNotEmpty(params.textStandard.author)) {
            Query q = new TermQuery(new Term(lucene.fieldSentenceTextVolume.name(),
                    params.textUnprocessed.volume));
            query.add(q, BooleanClause.Occur.MUST);
        }
    }

    public Term getLemmaTerm(String lemma) {
        return new Term(lucene.fieldSentenceLemmas.name(), lemma);
    }

    public Term getValueTerm(String value) {
        return new Term(lucene.fieldSentenceValues.name(), value);
    }

    public Term getGrammarTerm(String grammar) {
        return new Term(lucene.fieldSentenceDBGrammarTags.name(), grammar);
    }

    public ScoreDoc[] search(Query query, ScoreDoc latest, DocFilter filter) throws Exception {
        return lucene.search(query, latest, filter);
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
}
