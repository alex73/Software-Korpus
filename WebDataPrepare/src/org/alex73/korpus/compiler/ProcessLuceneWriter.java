package org.alex73.korpus.compiler;

import java.nio.file.Paths;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.server.engine.LuceneFields;
import org.alex73.korpus.server.engine.StringArrayTokenStream;
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

public class ProcessLuceneWriter extends BaseParallelProcessor {
    protected Directory dir;

    private final LuceneFields fields = new LuceneFields();
    protected IndexWriter indexWriter;

    protected Document docSentence;

    public ProcessLuceneWriter(boolean realWrite, String rootDir, int bufferSizeMb) throws Exception {
        super(1, 40);
        defaultThreadPriority = Thread.MAX_PRIORITY;
        IndexWriterConfig config = new IndexWriterConfig();
        config.setOpenMode(OpenMode.CREATE);
        config.setRAMBufferSizeMB(bufferSizeMb);
        config.setUseCompoundFile(false);
        config.setIndexSort(new Sort(new SortField(fields.fieldTextID.name(), SortField.Type.INT)));

        if (realWrite) {
            dir = new NIOFSDirectory(Paths.get(rootDir));
            indexWriter = new IndexWriter(dir, config);
        }

        docSentence = new Document();
        docSentence.add(fields.fieldSentenceValues);
        docSentence.add(fields.fieldSentenceDBGrammarTags);
        docSentence.add(fields.fieldSentenceLemmas);
        docSentence.add(fields.fieldSentencePBinary);
        docSentence.add(fields.fieldSentencePage);

        docSentence.add(fields.fieldSentenceTextSubcorpus);
        // docSentence.add(fieldSentenceTextIDOrder);
        docSentence.add(fields.fieldSentenceTextStyleGenre);
        docSentence.add(fields.fieldSentenceTextAuthor);
        docSentence.add(fields.fieldSentenceTextSource);
        docSentence.add(fields.fieldSentenceTextCreationYear);
        docSentence.add(fields.fieldSentenceTextPublishedYear);

        docSentence.add(fields.fieldTextID);
    }

    public synchronized void shutdown() throws Exception {
        if (indexWriter != null) {
            indexWriter.forceMerge(1, true);
            indexWriter.commit();
            indexWriter.close();
            dir.close();
        }
    }

    public void process(TextInfo textInfo, int page, String[] values, String[] dbGrammarTags, String[] lemmas,
            byte[] xml) {
        run(() -> {
            addSentence(textInfo, page, values, dbGrammarTags, lemmas, xml);
        });
    }

    /**
     * Add sentence to database. Sentence linked to previously added text.
     * 
     * @return words count
     */
    private synchronized void addSentence(TextInfo textInfo, int page, String[] values, String[] dbGrammarTags,
            String[] lemmas, byte[] xml) throws Exception {
        fields.fieldSentenceTextSubcorpus.setStringValue(textInfo.subcorpus);
        fields.fieldSentenceTextStyleGenre.setTokenStream(new StringArrayTokenStream(textInfo.styleGenres));
        setYearsRange(textInfo.creationTime, fields.fieldSentenceTextCreationYear);
        setYearsRange(textInfo.publicationTime, fields.fieldSentenceTextPublishedYear);
        fields.fieldSentenceTextAuthor.setTokenStream(new StringArrayTokenStream(textInfo.authors));
        fields.fieldSentenceTextSource.setStringValue(textInfo.source != null ? textInfo.source : "");

        fields.fieldSentenceValues.setTokenStream(new StringArrayTokenStream(values));
        fields.fieldSentenceDBGrammarTags.setTokenStream(new StringArrayTokenStream(dbGrammarTags));
        fields.fieldSentenceLemmas.setTokenStream(new StringArrayTokenStream(lemmas));
        fields.fieldSentencePBinary.setBytesValue(xml);
        fields.fieldSentencePage.setIntValue(page);

        Integer position = PrepareCache3.textPositionsBySourceFile.get(textInfo.sourceFilePath);
        if (position == null) {
            System.out.println("No file " + textInfo.sourceFilePath);
        }
        fields.fieldTextID.setIntValue(position); // TODO

        if (indexWriter != null) {
            indexWriter.addDocument(docSentence);
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
