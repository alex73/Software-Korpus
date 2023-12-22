package org.alex73.korpus.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
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

public class Step5WriteLucene {
    private static final Logger LOG = LoggerFactory.getLogger(Step5WriteLucene.class);

    private static Path rootDir;
    private static String[] allLanguages;
    private static boolean realWrite;
    private static boolean cacheForProduction;
    private static int bufferSizeMb;
    private static double bufferSizeMbEachInstance;

    private static final String[] EMPTY_STRINGARRAY = new String[0];
    private static final StringArrayTokenStream EMPTY_STRINGARRAY_VALUE = new StringArrayTokenStream(new String[0]);

    private static LinkedList<LuceneContext> lucenePool = new LinkedList<>();
    private static AtomicInteger lucenePoolIndex = new AtomicInteger();

    private static LuceneContext getLuceneContext() {
        LuceneContext r;
        synchronized (lucenePool) {
            r = lucenePool.poll();
        }
        if (r == null) {
            r = new LuceneContext();
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
                    r.path = rootDir.resolve("lucene-" + lucenePoolIndex.addAndGet(1));
                    r.dir = new NIOFSDirectory(r.path);
                    r.indexWriter = new IndexWriter(r.dir, config);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return r;
    }

    public static void init(int instanceCount, String[] languages, boolean rw, boolean cfp, Path outDir, int bs) {
        allLanguages = languages;
        realWrite = rw;
        cacheForProduction = cfp;
        rootDir = outDir;
        bufferSizeMb = bs;
        bufferSizeMbEachInstance = bufferSizeMb * 1.0 / instanceCount;
        LOG.info("Lucene will use " + bufferSizeMb + "mb");
    }

    public static void run(MessageLuceneWrite text) throws Exception {
        try (LuceneContext c = getLuceneContext()) {
            grow(c, text.paragraphs.length);
            for (int i = 0; i < text.paragraphs.length; i++) {
                LuceneFields fields = c.fields.get(i);
                for (LuceneFields.LuceneFieldsLang lf : fields.byLang.values()) {
                    lf.fieldWordWriteVariant.setTokenStream(EMPTY_STRINGARRAY_VALUE);
                    lf.fieldTagsWriteVariant.setTokenStream(EMPTY_STRINGARRAY_VALUE);

                    if (lf.fieldTextAuthor != null) {
                        lf.fieldTextAuthor.setTokenStream(EMPTY_STRINGARRAY_VALUE);
                    }
                    if (lf.fieldTextSource != null) {
                        lf.fieldTextSource.setStringValue("");
                    }
                    if (lf.fieldTextCreationYear != null) {
                        setYearsRange(null, lf.fieldTextCreationYear);
                    }
                    if (lf.fieldTextPublishedYear != null) {
                        setYearsRange(null, lf.fieldTextPublishedYear);
                    }
                }

                LuceneParagraph p = text.paragraphs[i];
                fields.fieldTextSubcorpus.setStringValue(text.textInfo.subcorpus);
                fields.fieldTextStyleGenre.setTokenStream(new StringArrayTokenStream(text.textInfo.styleGenres));

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
                    fl.fieldWordWriteVariant.setTokenStream(new StringArrayTokenStream(en.getValue().values));
                    fl.fieldTagsWriteVariant.setTokenStream(new StringArrayTokenStream(en.getValue().dbGrammarTags));
                    if (fl.fieldTextAuthor != null) {
                        fl.fieldTextAuthor.setTokenStream(new StringArrayTokenStream(langAuthors.toArray(EMPTY_STRINGARRAY)));
                    }
                    if (fl.fieldTextSource != null) {
                        fl.fieldTextSource.setTokenStream(new StringArrayTokenStream(langSources.toArray(EMPTY_STRINGARRAY)));
                    }
                    if (fl.fieldTextCreationYear != null) {
                        setYearsRange(langCreationTime, fl.fieldTextCreationYear);
                    }
                    if (fl.fieldTextPublishedYear != null) {
                        setYearsRange(langPublicationTime, fl.fieldTextPublishedYear);
                    }
                }
                fields.fieldSentencePBinary.setBytesValue(p.xml);
                fields.fieldTextID.setIntValue(text.textInfo.globalTextOrder);
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
        }
    }

    public static void finish() throws Exception {
        lucenePool.parallelStream().forEach(c -> c.forget());
    }

    private static void setYearsRange(String date, IntRange rangeField) {
        if (date == null || date.isEmpty()) {
            rangeField.setRangeValues(new int[] { Integer.MAX_VALUE }, new int[] { Integer.MAX_VALUE });
        } else {
            KorpusDateTime dt = new KorpusDateTime(date);
            rangeField.setRangeValues(new int[] { dt.getEarliestYear() }, new int[] { dt.getLatestYear() });
        }
    }

    private static void grow(LuceneContext c, int capacity) {
        while (c.fields.size() < capacity) {
            c.fields.add(new LuceneFields(allLanguages));
        }
        while (c.documents.size() < capacity) {
            LuceneFields fields = c.fields.get(c.documents.size());
            Document docSentence = new Document();
            for (LuceneFields.LuceneFieldsLang lf : fields.byLang.values()) {
                docSentence.add(lf.fieldWordWriteVariant);
                docSentence.add(lf.fieldTagsWriteVariant);
                if (lf.fieldTextAuthor != null) {
                    docSentence.add(lf.fieldTextAuthor);
                }
                if (lf.fieldTextSource != null) {
                    docSentence.add(lf.fieldTextSource);
                }
                if (lf.fieldTextCreationYear != null) {
                    docSentence.add(lf.fieldTextCreationYear);
                }
                if (lf.fieldTextPublishedYear != null) {
                    docSentence.add(lf.fieldTextPublishedYear);
                }
            }
            docSentence.add(fields.fieldSentencePBinary);

            docSentence.add(fields.fieldTextSubcorpus);
            docSentence.add(fields.fieldTextStyleGenre);

            docSentence.add(fields.fieldTextID);
            c.documents.add(docSentence);
        }
    }

    public static void mergeIndexes() throws Exception {
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
                partDirs = lucenePool.stream().map(c -> {
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
        for (LuceneContext c : lucenePool) {
            try (Stream<Path> ls = Files.list(c.path)) {
                for (Path f : ls.toList()) {
                    Files.delete(f);
                }
            }
            Files.delete(c.path);
        }
    }

    static class LuceneContext implements AutoCloseable {
        Path path;
        IndexWriter indexWriter;
        NIOFSDirectory dir;
        List<LuceneFields> fields = new ArrayList<>();
        List<Document> documents = new ArrayList<>();

        void forget() {
            if (indexWriter != null) {
                try {
                    indexWriter.close();
                    dir.close();
                    indexWriter = null;
                    dir = null;
                    documents.clear();
                    documents = null;
                    fields.clear();
                    fields = null;
                } catch (Throwable ex) {
                    LOG.error("", ex);
                    throw new RuntimeException(ex);
                }
            }
        }

        @Override
        public void close() throws Exception {
            synchronized (lucenePool) {
                lucenePool.add(this);
            }
        }
    }
}
