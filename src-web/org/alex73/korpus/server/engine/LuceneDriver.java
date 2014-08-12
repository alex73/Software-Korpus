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

import java.io.File;
import java.util.Arrays;

import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.server.Settings;
import org.alex73.korpus.shared.StyleGenres;
import org.alex73.korpus.utils.WordNormalizer;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.NumericTokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

import alex73.corpus.paradigm.S;
import alex73.corpus.paradigm.W;

/**
 * Lucene driver for corpus document's database.
 */
public class LuceneDriver {
    static final Logger LOGGER = LogManager.getLogger(LuceneDriver.class);

    private Directory dir;
    private IndexWriter indexWriter;
    private DirectoryReader directoryReader;
    private IndexSearcher indexSearcher;
    private Document docSentence, docText;

    public Field fieldSentenceTextID;
    public Field fieldSentenceValues;
    public Field fieldSentenceDBGrammarTags;
    public Field fieldSentenceLemmas;
    public Field fieldSentenceXML;
    public Field fieldSentenceTextStyleGenre;
    public Field fieldSentenceTextAuthor;
    public Field fieldSentenceTextWrittenYear;
    public Field fieldSentenceTextPublishedYear;

    public Field fieldTextID;
    public Field fieldTextAuthors;
    public Field fieldTextTitle;
    public Field fieldTextYearWritten;
    public Field fieldTextYearPublished;

    private StringBuilder values = new StringBuilder();
    private StringBuilder dbGrammarTags = new StringBuilder();
    private StringBuilder lemmas = new StringBuilder();

    public static final FieldType TYPE_STORED_NOTINDEXED = new FieldType();
    static {
        TYPE_STORED_NOTINDEXED.setIndexed(false);
        TYPE_STORED_NOTINDEXED.setTokenized(false);
        TYPE_STORED_NOTINDEXED.setOmitNorms(true);
        TYPE_STORED_NOTINDEXED.setIndexOptions(IndexOptions.DOCS_ONLY);
        TYPE_STORED_NOTINDEXED.setStored(true);
        TYPE_STORED_NOTINDEXED.freeze();
    }
    public static final FieldType TYPE_STORED_NOTINDEXED_INT = new FieldType();
    static {
        TYPE_STORED_NOTINDEXED_INT.setIndexed(false);
        TYPE_STORED_NOTINDEXED_INT.setTokenized(false);
        TYPE_STORED_NOTINDEXED_INT.setOmitNorms(true);
        TYPE_STORED_NOTINDEXED_INT.setIndexOptions(IndexOptions.DOCS_ONLY);
        TYPE_STORED_NOTINDEXED_INT.setNumericType(FieldType.NumericType.INT);
        TYPE_STORED_NOTINDEXED_INT.setStored(true);
        TYPE_STORED_NOTINDEXED_INT.freeze();
    }
    public static final FieldType TYPE_NOTSTORED_INDEXED_INT = new FieldType();
    static {
        TYPE_NOTSTORED_INDEXED_INT.setIndexed(true);
        TYPE_NOTSTORED_INDEXED_INT.setTokenized(false);
        TYPE_NOTSTORED_INDEXED_INT.setOmitNorms(true);
        TYPE_NOTSTORED_INDEXED_INT.setIndexOptions(IndexOptions.DOCS_ONLY);
        TYPE_NOTSTORED_INDEXED_INT.setNumericType(FieldType.NumericType.INT);
        TYPE_NOTSTORED_INDEXED_INT.setStored(false);
        TYPE_NOTSTORED_INDEXED_INT.freeze();
    }
    public static final FieldType TYPE_NOTSTORED_INDEXED = new FieldType();
    static {
        TYPE_NOTSTORED_INDEXED.setIndexed(true);
        TYPE_NOTSTORED_INDEXED.setTokenized(true);
        TYPE_NOTSTORED_INDEXED.setOmitNorms(true);
        TYPE_NOTSTORED_INDEXED.setIndexOptions(IndexOptions.DOCS_ONLY);
        TYPE_NOTSTORED_INDEXED.setStored(false);
        TYPE_NOTSTORED_INDEXED.freeze();
    }

    public LuceneDriver(File rootDir, boolean write) throws Exception {
        dir = new NIOFSDirectory(rootDir);

        if (write) {
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_9, new WhitespaceAnalyzer(
                    Version.LUCENE_4_9));
            config.setOpenMode(OpenMode.CREATE);
            config.setRAMBufferSizeMB(256);
            indexWriter = new IndexWriter(dir, config);
        } else {
            directoryReader = DirectoryReader.open(dir);
            indexSearcher = new IndexSearcher(directoryReader);
        }

        docSentence = new Document();

        docSentence.add(fieldSentenceValues = new Field("value", "", TYPE_NOTSTORED_INDEXED));
        docSentence.add(fieldSentenceDBGrammarTags = new Field("dbGrammarTag", "", TYPE_NOTSTORED_INDEXED));
        docSentence.add(fieldSentenceLemmas = new Field("lemma", "", TYPE_NOTSTORED_INDEXED));
        docSentence.add(fieldSentenceXML = new Field("xml", new byte[0], TYPE_STORED_NOTINDEXED));
        docSentence.add(fieldSentenceTextID = new IntField("textId", 0, TYPE_STORED_NOTINDEXED_INT));
        docSentence.add(fieldSentenceTextStyleGenre = new Field("textStyleGenre", "", TYPE_NOTSTORED_INDEXED));
        docSentence.add(fieldSentenceTextAuthor = new Field("textAuthor", "", TYPE_NOTSTORED_INDEXED));
        docSentence.add(fieldSentenceTextWrittenYear = new IntField("writtenYear", 0, TYPE_NOTSTORED_INDEXED_INT));
        docSentence.add(fieldSentenceTextPublishedYear = new IntField("publishedYear", 0, TYPE_NOTSTORED_INDEXED_INT));

        docText = new Document();
        docText.add(fieldTextID = new IntField("id", 0, TYPE_NOTSTORED_INDEXED_INT));
        docText.add(fieldTextAuthors = new Field("authors", "", TYPE_STORED_NOTINDEXED));
        docText.add(fieldTextTitle = new Field("title", "", TYPE_STORED_NOTINDEXED));
        docText.add(fieldTextYearWritten = new IntField("textYearWritten", 0, TYPE_STORED_NOTINDEXED_INT));
        docText.add(fieldTextYearPublished = new IntField("textYearPublished", 0, TYPE_STORED_NOTINDEXED_INT));
    }

    /**
     * Add text to database.
     */
    public void addText(int textId, TextInfo info) throws Exception {
        fieldTextID.setIntValue(textId);
        fieldTextAuthors.setStringValue(merge(info.authors,";"));
        fieldTextTitle.setStringValue(info.title);
        fieldTextYearWritten.setIntValue(info.writtenYear!=null?info.writtenYear:0);
        fieldTextYearPublished.setIntValue(info.publishedYear!=null?info.publishedYear:0);
        indexWriter.addDocument(docText);
    }

    /**
     * Add sentence to database. Sentence linked to previously added text.
     */
    public void addSentence(S sentence, byte[] xml, int textId, TextInfo info) throws Exception {
        values.setLength(0);
        lemmas.setLength(0);
        dbGrammarTags.setLength(0);

        for (Object o : sentence.getWOrTag()) {
            if (!(o instanceof W)) {
                continue;
            }
            W w = (W) o;
            if (w.getValue() != null) {
                String wc = WordNormalizer.normalize(w.getValue());
                values.append(wc).append(' ');
            }
            if (StringUtils.isNotEmpty(w.getCat())) {
                for (String t : w.getCat().split("_")) {
                    if (!BelarusianTags.getInstance().isValid(t, null)) {
                        //TODO throw new Exception("Няправільны тэг: " + t);
                    } else {
                        dbGrammarTags.append(DBTagsGroups.getDBTagString(t)).append(' ');
                    }
                }
            }
            if (w.getLemma() != null) {
                lemmas.append(w.getLemma().replace('_', ' ')).append(' ');
            }
        }

        // fieldID.setIntValue(id);
        fieldSentenceTextID.setIntValue(textId);
        fieldSentenceValues.setStringValue(values.toString());
        fieldSentenceDBGrammarTags.setStringValue(dbGrammarTags.toString());
        fieldSentenceLemmas.setStringValue(lemmas.toString());
        fieldSentenceXML.setBytesValue(xml);
        fieldSentenceTextStyleGenre.setTokenStream(new StringArrayTokenStream(info.styleGenres));
        fieldSentenceTextWrittenYear.setIntValue(nvl(info.writtenYear));
        fieldSentenceTextPublishedYear.setIntValue(nvl(info.publishedYear));
        fieldSentenceTextAuthor.setTokenStream(new StringArrayTokenStream(info.authors));

        indexWriter.addDocument(docSentence);
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

    public String nvl(String n) {
        return n!=null?n:"";
    }

    public int nvl(Integer n) {
        return n!=null?n:0;
    }
    
    String merge(String[] strs,String sep) {
        StringBuilder out=new StringBuilder();
        for(String s:strs) {
            if (out.length()>0) {
                out.append(sep);
            }
            out.append(s);
        }
        return out.toString();
    }

    public TextInfo getTextInfo(int textId) throws Exception {
        NumericRangeQuery<Integer> query = NumericRangeQuery.newIntRange(fieldTextID.name(), 1, textId, textId, true,
                true);
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

    public Document getSentence(int docID) throws Exception {
        return indexSearcher.doc(docID);
    }

    public void shutdown() throws Exception {
        if (directoryReader != null)
            directoryReader.close();
        if (indexWriter != null) {
            indexWriter.forceMerge(1);
            indexWriter.commit();
            indexWriter.close();
        }
        dir.close();
    }

    public interface DocFilter {
        public boolean isDocAllowed(int docID);
    }
}
