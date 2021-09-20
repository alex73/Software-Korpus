package org.alex73.korpus.compiler;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.server.engine.LuceneFields;
import org.alex73.korpus.server.engine.StringArrayTokenStream;
import org.alex73.korpus.utils.KorpusDateTime;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntRange;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.NoMergePolicy;
import org.apache.lucene.index.NoMergeScheduler;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessLuceneWriter extends BaseParallelProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessLuceneWriter.class);

    static final int INSTANCE_COUNT = 8;

    private final Path rootDir;

    private final boolean realWrite;
    private final boolean cacheForProduction;
    private final int bufferSizeMb;
    private final double bufferSizeMbEachInstance;
    private final List<LuceneContext> contexts = Collections.synchronizedList(new ArrayList<>());

    private ThreadLocal<LuceneContext> context = new ThreadLocal<>() {
        @Override
        protected LuceneContext initialValue() {
            LuceneContext r = new LuceneContext();
            contexts.add(r);
            r.fields = new LuceneFields();
            if (realWrite) {
                IndexWriterConfig config = new IndexWriterConfig();
                config.setCommitOnClose(true);
                config.setOpenMode(OpenMode.CREATE);
                config.setRAMBufferSizeMB(bufferSizeMbEachInstance);
                config.setIndexSort(new Sort(new SortField(r.fields.fieldTextID.name(), SortField.Type.INT)));
                config.setMergePolicy(NoMergePolicy.INSTANCE);
                config.setMergeScheduler(NoMergeScheduler.INSTANCE);
                try {
                    r.path = rootDir.resolve(Thread.currentThread().getName());
                    r.dir = new NIOFSDirectory(r.path);
                    r.indexWriter = new IndexWriter(r.dir, config);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }

            r.docSentence = new Document();
            r.docSentence.add(r.fields.fieldSentenceValues);
            r.docSentence.add(r.fields.fieldSentenceDBGrammarTags);
            r.docSentence.add(r.fields.fieldSentenceLemmas);
            r.docSentence.add(r.fields.fieldSentencePBinary);
            r.docSentence.add(r.fields.fieldSentencePage);

            r.docSentence.add(r.fields.fieldSentenceTextSubcorpus);
            // docSentence.add(fieldSentenceTextIDOrder);
            r.docSentence.add(r.fields.fieldSentenceTextStyleGenre);
            r.docSentence.add(r.fields.fieldSentenceTextAuthor);
            r.docSentence.add(r.fields.fieldSentenceTextSource);
            r.docSentence.add(r.fields.fieldSentenceTextCreationYear);
            r.docSentence.add(r.fields.fieldSentenceTextPublishedYear);

            r.docSentence.add(r.fields.fieldTextID);
            return r;
        }
    };

    public ProcessLuceneWriter(boolean realWrite, boolean cacheForProduction, String rootDir, int bufferSizeMb)
            throws Exception {
        super(INSTANCE_COUNT, 40);
        LOG.info("Lucene will use " + bufferSizeMb + "mb");
        this.realWrite = realWrite;
        this.cacheForProduction = cacheForProduction;
        this.rootDir = Paths.get(rootDir);
        this.bufferSizeMb = bufferSizeMb;
        this.bufferSizeMbEachInstance = bufferSizeMb * 1.0 / INSTANCE_COUNT;
        defaultThreadPriority = Thread.MAX_PRIORITY;
    }

    @Override
    public void finish(int minutes) throws Exception {
        super.finish(minutes);
        contexts.parallelStream().forEach(c -> {
            if (c.indexWriter != null) {
                try {
                    c.indexWriter.close();
                    c.dir.close();
                } catch (Throwable ex) {
                    LOG.error("", ex);
                    throw new RuntimeException(ex);
                }
            }
        });
        if (realWrite) {
            LOG.info("Merge all...");
            mergeIndexes();
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
    private void addSentence(TextInfo textInfo, int page, String[] values, String[] dbGrammarTags, String[] lemmas,
            byte[] xml) throws Exception {
        LuceneContext c = context.get();
        c.fields.fieldSentenceTextSubcorpus.setStringValue(textInfo.subcorpus);
        c.fields.fieldSentenceTextStyleGenre.setTokenStream(new StringArrayTokenStream(textInfo.styleGenres));
        setYearsRange(textInfo.creationTime, c.fields.fieldSentenceTextCreationYear);
        setYearsRange(textInfo.publicationTime, c.fields.fieldSentenceTextPublishedYear);
        c.fields.fieldSentenceTextAuthor.setTokenStream(new StringArrayTokenStream(textInfo.authors));
        c.fields.fieldSentenceTextSource.setStringValue(textInfo.source != null ? textInfo.source : "");

        c.fields.fieldSentenceValues.setTokenStream(new StringArrayTokenStream(values));
        c.fields.fieldSentenceDBGrammarTags.setTokenStream(new StringArrayTokenStream(dbGrammarTags));
        c.fields.fieldSentenceLemmas.setTokenStream(new StringArrayTokenStream(lemmas));
        c.fields.fieldSentencePBinary.setBytesValue(xml);
        c.fields.fieldSentencePage.setIntValue(page);

        Integer position = PrepareCache3.textPositionsBySourceFile.get(textInfo.sourceFilePath);
        if (position == null) {
            throw new Exception("No file " + textInfo.sourceFilePath);
        }
        c.fields.fieldTextID.setIntValue(position); // TODO

        if (c.indexWriter != null) {
            c.indexWriter.addDocument(c.docSentence);
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

    public void mergeIndexes() throws Exception {
        LuceneFields fields = new LuceneFields();
        IndexWriterConfig config = new IndexWriterConfig();
        config.setCommitOnClose(true);
        config.setOpenMode(OpenMode.CREATE);
        config.setRAMBufferSizeMB(bufferSizeMb);
        config.setIndexSort(new Sort(new SortField(fields.fieldTextID.name(), SortField.Type.INT)));
        config.setMergeScheduler(new ConcurrentMergeScheduler());

        Directory[] partDirs;
        try (Directory dir = new NIOFSDirectory(rootDir)) {
            try (IndexWriter indexWriter = new IndexWriter(dir, config)) {
                partDirs = contexts.stream().map(c -> {
                    try {
                        return new NIOFSDirectory(c.path);
                    } catch (IOException ex) {
                        LOG.error("", ex);
                        throw new RuntimeException(ex);
                    }
                }).toArray(Directory[]::new);
                indexWriter.addIndexes(partDirs);
                if (cacheForProduction) {
                    indexWriter.forceMerge(1, true);
                }
            }
        }
        for (Directory d : partDirs) {
            d.close();
        }
    }

    static class LuceneContext {
        LuceneFields fields;
        IndexWriter indexWriter;
        Path path;
        NIOFSDirectory dir;
        Document docSentence;
    }
}
