package org.alex73.korpus.compiler;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alex73.korpus.base.BelarusianWordNormalizer;
import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.server.engine.LuceneFields;
import org.alex73.korpus.server.engine.StringArrayTokenStream;
import org.alex73.korpus.server.text.BinaryParagraphWriter;
import org.alex73.korpus.text.elements.Paragraph;
import org.alex73.korpus.text.elements.Sentence;
import org.alex73.korpus.text.elements.Word;
import org.alex73.korpus.utils.KorpusDateTime;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntRange;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

public class LuceneDriverWrite extends LuceneFields {
    static final String[] STRING_ARRAY = new String[0];

    protected Directory dir;

    protected IndexWriter indexWriter;

    protected Document docSentence;

    public LuceneDriverWrite(String rootDir) throws Exception {
        IndexWriterConfig config = new IndexWriterConfig();
        config.setOpenMode(OpenMode.CREATE);
        config.setRAMBufferSizeMB(2048);
        config.setIndexSort(new Sort(new SortField(fieldTextID.name(), SortField.Type.INT)));

        dir = new NIOFSDirectory(Paths.get(rootDir));
        indexWriter = new IndexWriter(dir, config);

        docSentence = new Document();
        docSentence.add(fieldSentenceValues);
        docSentence.add(fieldSentenceDBGrammarTags);
        docSentence.add(fieldSentenceLemmas);
        docSentence.add(fieldSentencePBinary);

        docSentence.add(fieldSentenceTextSubcorpus);
        // docSentence.add(fieldSentenceTextIDOrder);
        docSentence.add(fieldSentenceTextStyleGenre);
        docSentence.add(fieldSentenceTextAuthor);
        docSentence.add(fieldSentenceTextSource);
        docSentence.add(fieldSentenceTextCreationYear);
        docSentence.add(fieldSentenceTextPublishedYear);

        docSentence.add(fieldTextID);
    }

    public synchronized void shutdown() throws Exception {
        indexWriter.forceMerge(1, true);
        indexWriter.commit();
        indexWriter.close();
        dir.close();
    }

    /**
     * Add sentence to database. Sentence linked to previously added text.
     * 
     * @return words count
     */
    protected synchronized void addSentence(TextInfo textInfo, String[] values, String[] dbGrammarTags, String[] lemmas,
            byte[] xml) throws Exception {
        fieldSentenceTextSubcorpus.setStringValue(textInfo.subcorpus);
        fieldSentenceTextStyleGenre.setTokenStream(new StringArrayTokenStream(textInfo.styleGenres));
        setYearsRange(textInfo.creationTime, fieldSentenceTextCreationYear);
        setYearsRange(textInfo.publicationTime, fieldSentenceTextPublishedYear);
        fieldSentenceTextAuthor.setTokenStream(new StringArrayTokenStream(textInfo.authors));
        fieldSentenceTextSource.setStringValue(textInfo.source != null ? textInfo.source : "");

        fieldSentenceValues.setTokenStream(new StringArrayTokenStream(values));
        fieldSentenceDBGrammarTags.setTokenStream(new StringArrayTokenStream(dbGrammarTags));
        fieldSentenceLemmas.setTokenStream(new StringArrayTokenStream(lemmas));
        fieldSentencePBinary.setBytesValue(xml);

        fieldTextID.setIntValue(PrepareCache3.textPositionsBySourceFile.get(textInfo.sourceFilePath));

        if (PrepareCache3.writeToLucene) {
            indexWriter.addDocument(docSentence);
        }
    }

    public void addSentences(TextInfo textInfo, List<Paragraph> content) throws Exception {
        BinaryParagraphWriter pwr = new BinaryParagraphWriter();
        Set<String> values = new HashSet<>();
        Set<String> dbGrammarTags = new HashSet<>();
        Set<String> lemmas = new HashSet<>();
        for (Paragraph p : content) {
            values.clear();
            dbGrammarTags.clear();
            lemmas.clear();
            for (Sentence se : p.sentences) {
                for (Word w : se.words) {
                    String wc = BelarusianWordNormalizer.superNormalized(w.lightNormalized);
                    values.add(wc);
                    if (w.tags != null && !w.tags.isEmpty()) {
                        for (String t : w.tags.split(";")) {
                            dbGrammarTags.add(DBTagsGroups.getDBTagString(t));
                        }
                    }
                    if (w.lemmas != null && !w.lemmas.isEmpty()) {
                        for (String t : w.lemmas.split(";")) {
                            lemmas.add(t);
                        }
                    }
                }
            }
            byte[] pxml = pwr.write(p);
            addSentence(textInfo, values.toArray(STRING_ARRAY), dbGrammarTags.toArray(STRING_ARRAY),
                    lemmas.toArray(STRING_ARRAY), pxml);
        }
    }

    private void setYearsRange(String date, IntRange rangeField) {
        if (date == null) {
            rangeField.setRangeValues(new int[] { Integer.MIN_VALUE }, new int[] { Integer.MIN_VALUE });
        } else {
            KorpusDateTime dt = new KorpusDateTime(date);
            rangeField.setRangeValues(new int[] { dt.getEarliestYear() }, new int[] { dt.getLatestYear() });
        }
    }
}
