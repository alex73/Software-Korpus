package org.alex73.korpus.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.MessageLuceneWrite.LuceneParagraph;
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

public class ProcessLuceneWriter extends BaseParallelProcessor<MessageLuceneWrite> {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessLuceneWriter.class);

    static final int INSTANCE_COUNT = 8;

    private final Path rootDir;
    private final Set<String> allLanguages;
    private final boolean realWrite;
    private final boolean cacheForProduction;
    private final int bufferSizeMb;
    private final double bufferSizeMbEachInstance;
    private final List<LuceneContext> contexts = Collections.synchronizedList(new ArrayList<>());

    private static final String[] EMPTY_STRINGARRAY = new String[0];
    private static final StringArrayTokenStream EMPTY_STRINGARRAY_VALUE = new StringArrayTokenStream(new String[0]);

    private ThreadLocal<LuceneContext> context = new ThreadLocal<>() {
        @Override
        protected LuceneContext initialValue() {
            LuceneContext r = new LuceneContext();
            contexts.add(r);
            if (realWrite) {
                IndexWriterConfig config = new IndexWriterConfig();
                config.setCommitOnClose(true);
                config.setOpenMode(OpenMode.CREATE);
                config.setRAMBufferSizeMB(bufferSizeMbEachInstance);
                // need to sort because mergeIndex will not work
                config.setIndexSort(new Sort(new SortField("textId", SortField.Type.INT)));
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
            return r;
        }
    };

    public ProcessLuceneWriter(Set<String> allLanguages, boolean realWrite, boolean cacheForProduction, Path rootDir, int bufferSizeMb) throws Exception {
        super(INSTANCE_COUNT, INSTANCE_COUNT * 3);
        LOG.info("Lucene will use " + bufferSizeMb + "mb");
        this.allLanguages = allLanguages;
        this.realWrite = realWrite;
        this.cacheForProduction = cacheForProduction;
        this.rootDir = rootDir;
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
    }

    @Override
    public void accept(MessageLuceneWrite text) {
        run(() -> {
            LuceneContext c = context.get();
            grow(text.paragraphs.length);
            for (int i = 0; i < text.paragraphs.length; i++) {
                LuceneFields fields = c.fields.get(i);
                for (LuceneFields.LuceneFieldsLang lf : fields.byLang.values()) {
                    lf.fieldSentenceValues.setTokenStream(EMPTY_STRINGARRAY_VALUE);
                    lf.fieldSentenceDBGrammarTags.setTokenStream(EMPTY_STRINGARRAY_VALUE);
                    lf.fieldSentenceLemmas.setTokenStream(EMPTY_STRINGARRAY_VALUE);

                    lf.fieldSentenceTextAuthor.setTokenStream(EMPTY_STRINGARRAY_VALUE);
                    lf.fieldSentenceTextSource.setStringValue("");
                    setYearsRange(null, lf.fieldSentenceTextCreationYear);
                    setYearsRange(null, lf.fieldSentenceTextPublishedYear);
                }

                LuceneParagraph p = text.paragraphs[i];
                fields.fieldSentenceTextSubcorpus.setStringValue(text.textInfo.subcorpus);
                fields.fieldSentenceTextStyleGenre.setTokenStream(new StringArrayTokenStream(text.textInfo.styleGenres));

                for (Map.Entry<String, MessageLuceneWrite.LuceneParagraphLang> en : p.byLang.entrySet()) {
                    String langCreationTime = "";
                    String langPublicationTime = "";
                    Set<String> langAuthors = new TreeSet<>();
                    Set<String> langSources = new TreeSet<>();
                    for (TextInfo.Subtext st : text.textInfo.subtexts) {
                        if (!en.getKey().equals(st.lang)) {
                            continue;
                        }
                        if (st.creationTime != null) {
                            langCreationTime += ';' + st.creationTime;
                        }
                        if (st.publicationTime != null) {
                            langPublicationTime += ';' + st.publicationTime;
                        }
                        if (st.authors != null) {
                            for (String a : st.authors) {
                                langAuthors.add(a);
                            }
                        }
                        if (st.source != null) {
                            langSources.add(st.source);
                        }
                    }
                    if (!langCreationTime.isEmpty()) {
                        langCreationTime = langCreationTime.substring(1);
                    }
                    if (!langPublicationTime.isEmpty()) {
                        langPublicationTime = langPublicationTime.substring(1);
                    }

                    LuceneFields.LuceneFieldsLang fl = fields.byLang.get(en.getKey());
                    fl.fieldSentenceValues.setTokenStream(new StringArrayTokenStream(en.getValue().values));
                    fl.fieldSentenceDBGrammarTags.setTokenStream(new StringArrayTokenStream(en.getValue().dbGrammarTags));
                    fl.fieldSentenceLemmas.setTokenStream(new StringArrayTokenStream(en.getValue().lemmas));
                    fl.fieldSentenceTextAuthor.setTokenStream(new StringArrayTokenStream(langAuthors.toArray(EMPTY_STRINGARRAY)));
                    fl.fieldSentenceTextSource.setTokenStream(new StringArrayTokenStream(langSources.toArray(EMPTY_STRINGARRAY)));
                    setYearsRange(langCreationTime, fl.fieldSentenceTextCreationYear);
                    setYearsRange(langPublicationTime, fl.fieldSentenceTextPublishedYear);
                }
                fields.fieldSentencePBinary.setBytesValue(p.xml);

                Integer position = PrepareCache3.textPositionsBySourceFile.get(text.textInfo.sourceFilePath);
                if (position == null) {
                    throw new RuntimeException("No file " + text.textInfo.sourceFilePath);
                }
                fields.fieldTextID.setIntValue(position); // TODO
            }

            if (c.indexWriter != null) {
                try {
                    // System.out.println("Add " + text.paragraphs.length + " for " +
                    // text.textInfo.title);
                    c.indexWriter.addDocuments(c.documents.subList(0, text.paragraphs.length));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    private void setYearsRange(String date, IntRange rangeField) {
        if (date == null || date.isEmpty()) {
            rangeField.setRangeValues(new int[] { Integer.MIN_VALUE }, new int[] { Integer.MAX_VALUE });
        } else {
            KorpusDateTime dt = new KorpusDateTime(date);
            rangeField.setRangeValues(new int[] { dt.getEarliestYear() }, new int[] { dt.getLatestYear() });
        }
    }

    private void grow(int capacity) {
        LuceneContext c = context.get();
        while (c.fields.size() < capacity) {
            c.fields.add(new LuceneFields(allLanguages));
        }
        while (c.documents.size() < capacity) {
            LuceneFields fields = c.fields.get(c.documents.size());
            Document docSentence = new Document();
            for (LuceneFields.LuceneFieldsLang lf : fields.byLang.values()) {
                docSentence.add(lf.fieldSentenceValues);
                docSentence.add(lf.fieldSentenceDBGrammarTags);
                docSentence.add(lf.fieldSentenceLemmas);
                docSentence.add(lf.fieldSentenceTextAuthor);
                docSentence.add(lf.fieldSentenceTextSource);
                docSentence.add(lf.fieldSentenceTextCreationYear);
                docSentence.add(lf.fieldSentenceTextPublishedYear);
            }
            docSentence.add(fields.fieldSentencePBinary);

            docSentence.add(fields.fieldSentenceTextSubcorpus);
            // docSentence.add(fieldSentenceTextIDOrder);
            docSentence.add(fields.fieldSentenceTextStyleGenre);

            docSentence.add(fields.fieldTextID);
            c.documents.add(docSentence);
        }
    }

    public ExRunnable mergeIndexes() {
        return () -> {
            LuceneFields fields = new LuceneFields(allLanguages);
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
            for (LuceneContext c : contexts) {
                try (Stream<Path> ls = Files.list(c.path)) {
                    for (Path f : ls.toList()) {
                        Files.delete(f);
                    }
                }
                Files.delete(c.path);
            }
        };
    }

    static class LuceneContext {
        IndexWriter indexWriter;
        Path path;
        NIOFSDirectory dir;
        List<LuceneFields> fields = new ArrayList<>();
        List<Document> documents = new ArrayList<>();
    }
}
