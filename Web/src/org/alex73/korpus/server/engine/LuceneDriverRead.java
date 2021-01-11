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
import java.util.ArrayList;
import java.util.List;

import org.alex73.korpus.server.data.LatestMark;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

/**
 * Lucene driver for corpus document's database.
 */
public class LuceneDriverRead extends LuceneFields {
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

    public <T> List<T> search(Query query, LatestMark latest, int maxResults, DocFilter<T> filter) throws Exception {
        System.out.println("   Lucene search: " + query);
        final List<T> result = new ArrayList<>(maxResults);

        TopDocs rs;
        // Sort sort = new Sort(new SortField(fieldSentenceTextRandomOrder.name(),
        // SortField.Type.INT, true));
        if (latest.doc == 0 && latest.shardIndex == 0) {
            rs = indexSearcher.search(query, maxResults, Sort.INDEXORDER);
        } else {
            FieldDoc latestDoc = new FieldDoc(latest.doc, latest.score, latest.fields, latest.shardIndex);
            rs = indexSearcher.searchAfter(latestDoc, query, maxResults, Sort.INDEXORDER);
            latest.doc = 0;
            latest.shardIndex = 0;
        }
        System.out.println("   Lucene found: total: " + rs.totalHits + ", block: " + rs.scoreDocs.length);
        while (rs.scoreDocs.length > 0) {
            List<Integer> pos = new ArrayList<>(rs.scoreDocs.length);
            List<T> resultPart = new ArrayList<>(rs.scoreDocs.length);
            for (int i = 0; i < rs.scoreDocs.length; i++) {
                pos.add(i);
                resultPart.add(null);
            }
            final ScoreDoc[] docs = rs.scoreDocs;
            // parallel check
            pos.parallelStream().forEach(p -> {
                try {
                    int id = docs[p].doc;
                    T res = filter.processDoc(id);
                    if (res != null) {
                        resultPart.set(p, res);
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            for (int i = 0; i < resultPart.size(); i++) {
                T res = resultPart.get(i);
                if (res != null) {
                    result.add(res);
                    if (result.size() >= maxResults) {
                        if (docs[i] instanceof FieldDoc) {
                            FieldDoc doc = (FieldDoc) docs[i];
                            latest.doc = doc.doc;
                            latest.score = doc.score;
                            latest.fields = doc.fields;
                            latest.shardIndex = doc.shardIndex;
                        } else {
                            ScoreDoc doc = docs[i];
                            latest.doc = doc.doc;
                            latest.score = doc.score;
                            latest.fields = null;
                            latest.shardIndex = doc.shardIndex;
                        }
                        break;
                    }
                }
            }
            if (result.size() < maxResults) {
                rs = indexSearcher.searchAfter(rs.scoreDocs[rs.scoreDocs.length - 1], query, maxResults, Sort.INDEXORDER);
                System.out.println("   Lucene found: block: " + rs.scoreDocs.length);
                System.out.println("found block " + rs.scoreDocs.length);
            } else {
                break;
            }
        }
        return result;
    }

    public void search(Query query, int pageSize, DocFilter<Void> filter) throws Exception {
        System.out.println("   Lucene search: " + query);

        TopDocs rs;
        rs = indexSearcher.search(query, pageSize, Sort.INDEXORDER);
        System.out.println("   Lucene found: total: " + rs.totalHits + ", block: " + rs.scoreDocs.length);
        while (rs.scoreDocs.length > 0) {
            List<Integer> docs = new ArrayList<>(rs.scoreDocs.length);
            for (int i = 0; i < rs.scoreDocs.length; i++) {
                docs.add(rs.scoreDocs[i].doc);
            }
            // parallel check
            docs.parallelStream().forEach(id -> {
                try {
                    filter.processDoc(id);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
            rs = indexSearcher.searchAfter(rs.scoreDocs[rs.scoreDocs.length - 1], query, pageSize, Sort.INDEXORDER);
            System.out.println("   Lucene found: block: " + rs.scoreDocs.length);
            System.out.println("found block " + rs.scoreDocs.length);
        }
    }

    public Document getSentence(int docID) throws Exception {
        return indexSearcher.doc(docID);
    }

    public interface DocFilter<T> {
        public T processDoc(int docID) throws Exception;
    }
}
