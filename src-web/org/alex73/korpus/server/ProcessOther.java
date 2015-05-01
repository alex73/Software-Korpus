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
import org.alex73.korpus.server.engine.LuceneDriverOther;
import org.alex73.korpus.server.engine.LuceneDriverKorpus.DocFilter;
import org.alex73.korpus.shared.SearchParams;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;

public class ProcessOther implements IProcess {
    LuceneDriverOther lucene;

    public ProcessOther(String dirPrefix) throws Exception {
        lucene = new LuceneDriverOther(dirPrefix + "/Other-cache/", false);
    }

    public void close() throws Exception {
        lucene.shutdown();
    }

    public void addTextQuery(BooleanQuery query, SearchParams params) {
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

    @Override
    public byte[] getXML(Document doc) {
        return doc.getField(lucene.fieldSentenceXML.name()).binaryValue().bytes;
    }

    public OtherInfo getOtherInfo(Document doc) throws Exception {
        OtherInfo info = new OtherInfo();
        info.textURL = doc.getField(lucene.fieldSentenceTextURL.name()).stringValue();
        return info;
    }
}
