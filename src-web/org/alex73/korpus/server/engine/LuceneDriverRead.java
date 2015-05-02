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

package org.alex73.korpus.server.engine;

import java.nio.file.Paths;
import java.util.Arrays;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.server.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

/**
 * Lucene driver for corpus document's database.
 */
public class LuceneDriverRead extends LuceneFields {
    protected final Logger LOGGER = LogManager.getLogger(LuceneDriverRead.class);

    protected Directory dir;

    protected DirectoryReader directoryReader;
    protected IndexSearcher indexSearcher;

    public LuceneDriverRead(String rootDir) throws Exception {
        dir = new NIOFSDirectory(Paths.get(rootDir));

        directoryReader = DirectoryReader.open(dir);
        indexSearcher = new IndexSearcher(directoryReader);
    }

    public void shutdown() throws Exception {
        directoryReader.close();
        dir.close();
    }

    public ScoreDoc[] search(Query query, ScoreDoc latest, DocFilter filter) throws Exception {
        LOGGER.info("   Lucene search: " + query);
        ScoreDoc[] result = new ScoreDoc[Settings.KORPUS_SEARCH_RESULT_PAGE + 1];

        int found = 0;

        TopDocs rs;
        if (latest == null) {
            rs = indexSearcher.search(query, Settings.KORPUS_SEARCH_RESULT_PAGE + 1);
        } else {
            rs = indexSearcher.searchAfter(latest, query, Settings.KORPUS_SEARCH_RESULT_PAGE + 1);
        }
        LOGGER.info("   Lucene found: total: " + rs.totalHits + ", block: " + rs.scoreDocs.length);
        while (rs.scoreDocs.length > 0 && found < result.length) {
            for (int i = 0; i < rs.scoreDocs.length && found < result.length; i++) {
                if (filter.isDocAllowed(rs.scoreDocs[i].doc)) {
                    result[found] = rs.scoreDocs[i];
                    found++;
                }
            }
            if (found < result.length) {
                rs = indexSearcher.searchAfter(rs.scoreDocs[rs.scoreDocs.length - 1], query,
                        Settings.KORPUS_SEARCH_RESULT_PAGE + 1);
                LOGGER.info("   Lucene found: block: " + rs.scoreDocs.length);
                System.out.println("found block " + rs.scoreDocs.length);
            }
        }

        return Arrays.copyOf(result, found);
    }

    public Document getSentence(int docID) throws Exception {
        return indexSearcher.doc(docID);
    }

    public TextInfo getTextInfo(int textId) throws Exception {
        NumericRangeQuery<Integer> query = NumericRangeQuery.newIntRange(fieldTextID.name(), 1, textId,
                textId, true, true);
        TopDocs rs = indexSearcher.search(query, 1);
        if (rs.totalHits < 1) {
            return null;
        }
        int v;
        Document doc = directoryReader.document(rs.scoreDocs[0].doc);
        TextInfo result = new TextInfo();
        result.authors = doc.getField(fieldTextAuthors.name()).stringValue().split(";");
        result.title = doc.getField(fieldTextTitle.name()).stringValue();
        v = doc.getField(fieldTextYearWritten.name()).numericValue().intValue();
        result.writtenYear = v > 0 ? v : null;
        v = doc.getField(fieldTextYearPublished.name()).numericValue().intValue();
        result.publishedYear = v > 0 ? v : null;
        return result;
    }

    public interface DocFilter {
        public boolean isDocAllowed(int docID);
    }
}
