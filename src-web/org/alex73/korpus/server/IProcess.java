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

import org.alex73.korpus.server.engine.LuceneDriverKorpus.DocFilter;
import org.alex73.korpus.shared.SearchParams;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;

public interface IProcess {

    void addTextQuery(BooleanQuery query, SearchParams params);

    Term getLemmaTerm(String lemma);

    Term getValueTerm(String value);

    Term getGrammarTerm(String grammar);

    ScoreDoc[] search(Query query, ScoreDoc latest, DocFilter filter) throws Exception;
    
    Document getSentence(int docID) throws Exception;
    
    byte[] getXML(Document doc);
}
