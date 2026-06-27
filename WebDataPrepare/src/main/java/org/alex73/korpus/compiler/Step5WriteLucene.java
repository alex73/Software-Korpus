package org.alex73.korpus.compiler;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.MessageLuceneWrite.LuceneParagraph;
import org.alex73.korpus.server.engine.LuceneFields;
import org.alex73.korpus.server.engine.StringArrayTokenStream;
import org.alex73.korpus.utils.KorpusDateTime;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

/**
 * Пяты этап кампіляцыі корпуса.
 * Клас забяспечвае непасрэдны запіс даных у індэксы Lucene: кіраванне індэксатарамі (IndexWriter),
 * размеркаванне памяці, падтрымка шматпаточнасці і фінальнае аб'яднанне (merge) сегментаў індэкса.
 */
public class Step5WriteLucene {
    private static final Logger LOG = LoggerFactory.getLogger(Step5WriteLucene.class);

    private static Path rootDir;
    private static String[] allLanguages;
    private static boolean realWrite;
    private static boolean cacheForProduction;
    private static int bufferSizeMb;

    private static IndexWriter globalIndexWriter;
    private static Directory globalDir;

    private static final String[] EMPTY_STRINGARRAY = new String[0];

    static class ReusableBuffer {
        LuceneFields fields;
        Document document;

        static class StreamsLang {
            StringArrayTokenStream word = new StringArrayTokenStream(EMPTY_STRINGARRAY);
            StringArrayTokenStream tags = new StringArrayTokenStream(EMPTY_STRINGARRAY);
            StringArrayTokenStream author = new StringArrayTokenStream(EMPTY_STRINGARRAY);
            StringArrayTokenStream source = new StringArrayTokenStream(EMPTY_STRINGARRAY);
        }

        Map<String, StreamsLang> streamsByLang = new HashMap<>();
        StringArrayTokenStream styleGenreStream = new StringArrayTokenStream(EMPTY_STRINGARRAY);

        ReusableBuffer() {
            fields = new LuceneFields(allLanguages);
            document = new Document();
            fields.fieldTextStyleGenre.setTokenStream(styleGenreStream);

            for (Map.Entry<String, LuceneFields.LuceneFieldsLang> e : fields.byLang.entrySet()) {
                LuceneFields.LuceneFieldsLang lf = e.getValue();
                StreamsLang sl = new StreamsLang();
                streamsByLang.put(e.getKey(), sl);

                lf.fieldWordWriteVariant.setTokenStream(sl.word);
                lf.fieldTagsWriteVariant.setTokenStream(sl.tags);
                if (lf.fieldTextAuthor != null) lf.fieldTextAuthor.setTokenStream(sl.author);
                if (lf.fieldTextSource != null) lf.fieldTextSource.setTokenStream(sl.source);

                document.add(lf.fieldWordWriteVariant);
                document.add(lf.fieldTagsWriteVariant);
                if (lf.fieldTextAuthor != null) {
                    document.add(lf.fieldTextAuthor);
                }
                if (lf.fieldTextSource != null) {
                    document.add(lf.fieldTextSource);
                }
                if (lf.fieldTextCreationYear != null) {
                    document.add(lf.fieldTextCreationYear);
                }
                if (lf.fieldTextPublishedYear != null) {
                    document.add(lf.fieldTextPublishedYear);
                }
            }
            document.add(fields.fieldSentencePBinary);
            document.add(fields.fieldTextSubcorpus);
            document.add(fields.fieldTextStyleGenre);
            document.add(fields.fieldTextID);
        }
    }

    private static final ThreadLocal<ReusableBuffer> threadBuffer = ThreadLocal.withInitial(ReusableBuffer::new);

    public static void init(String[] languages, boolean rw, boolean cfp, Path outDir, int bs) {
        allLanguages = languages;
        realWrite = rw;
        cacheForProduction = cfp;
        rootDir = outDir;
        bufferSizeMb = bs;
        LOG.info("Lucene will use " + bufferSizeMb + "mb");

        if (realWrite) {
            try {
                IndexWriterConfig config = new IndexWriterConfig();
                config.setCommitOnClose(true);
                config.setOpenMode(OpenMode.CREATE);
                config.setRAMBufferSizeMB(bufferSizeMb);

                // Speed up indexing
                config.setUseCompoundFile(false);
                TieredMergePolicy mergePolicy = new TieredMergePolicy();
                mergePolicy.setSegmentsPerTier(50);
                mergePolicy.setMaxMergeAtOnce(50);
                config.setMergePolicy(mergePolicy);

                config.setIndexSort(new Sort(new SortField("textId", SortField.Type.INT)));

                globalDir = FSDirectory.open(rootDir.resolve("lucene"));
                globalIndexWriter = new IndexWriter(globalDir, config);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static void run(MessageLuceneWrite text) throws Exception {
        Map<String, String[]> langAuthorsMap = new HashMap<>();
        Map<String, String[]> langSourcesMap = new HashMap<>();
        Map<String, int[]> cachedCreationRanges = new HashMap<>();
        Map<String, int[]> cachedPublicationRanges = new HashMap<>();

        if (text.textInfo != null && text.textInfo.subtexts != null) {
            Map<String, Set<String>> tempAuthors = new HashMap<>();
            Map<String, Set<String>> tempSources = new HashMap<>();
            Map<String, StringJoiner> tempCreation = new HashMap<>();
            Map<String, StringJoiner> tempPublication = new HashMap<>();

            for (TextInfo.Subtext st : text.textInfo.subtexts) {
                String lang = st.lang;
                if (lang == null) continue;

                if (st.creationTime != null) {
                    tempCreation.computeIfAbsent(lang, k -> new StringJoiner(";")).add(st.creationTime);
                }
                if (st.publicationTime != null) {
                    tempPublication.computeIfAbsent(lang, k -> new StringJoiner(";")).add(st.publicationTime);
                }
                if (st.authors != null) {
                    tempAuthors.computeIfAbsent(lang, k -> new TreeSet<>()).addAll(Arrays.asList(st.authors));
                }
                if (st.sourceName != null) {
                    tempSources.computeIfAbsent(lang, k -> new TreeSet<>()).add(st.sourceName);
                }
            }

            for (Map.Entry<String, Set<String>> e : tempAuthors.entrySet()) {
                langAuthorsMap.put(e.getKey(), e.getValue().toArray(EMPTY_STRINGARRAY));
            }
            for (Map.Entry<String, Set<String>> e : tempSources.entrySet()) {
                langSourcesMap.put(e.getKey(), e.getValue().toArray(EMPTY_STRINGARRAY));
            }
            for (Map.Entry<String, StringJoiner> e : tempCreation.entrySet()) {
                KorpusDateTime dt = new KorpusDateTime(e.getValue().toString());
                cachedCreationRanges.put(e.getKey(), new int[]{dt.getEarliestYear(), dt.getLatestYear()});
            }
            for (Map.Entry<String, StringJoiner> e : tempPublication.entrySet()) {
                KorpusDateTime dt = new KorpusDateTime(e.getValue().toString());
                cachedPublicationRanges.put(e.getKey(), new int[]{dt.getEarliestYear(), dt.getLatestYear()});
            }
        }

        ReusableBuffer b = threadBuffer.get();
        LuceneFields fields = b.fields;

        b.styleGenreStream.setData(text.textInfo.styleGenres);
        fields.fieldTextSubcorpus.setStringValue(text.textInfo.subcorpus);
        fields.fieldTextID.setIntValue(text.textInfo.globalTextOrder);

        for (int i = 0; i < text.paragraphs.length; i++) {
            LuceneParagraph p = text.paragraphs[i];

            for (Map.Entry<String, LuceneFields.LuceneFieldsLang> entry : fields.byLang.entrySet()) {
                String lang = entry.getKey();
                LuceneFields.LuceneFieldsLang lf = entry.getValue();
                ReusableBuffer.StreamsLang sl = b.streamsByLang.get(lang);

                MessageLuceneWrite.LuceneParagraphLang pLang = p.byLang.get(lang);

                if (pLang != null) {
                    sl.word.setData(pLang.values);
                    sl.tags.setData(pLang.dbGrammarTags);
                    if (lf.fieldTextAuthor != null)
                        sl.author.setData(langAuthorsMap.getOrDefault(lang, EMPTY_STRINGARRAY));
                    if (lf.fieldTextSource != null)
                        sl.source.setData(langSourcesMap.getOrDefault(lang, EMPTY_STRINGARRAY));

                    if (lf.fieldTextCreationYear != null) {
                        int[] range = cachedCreationRanges.get(lang);
                        if (range == null) {
                            lf.fieldTextCreationYear.setRangeValues(new int[]{Integer.MAX_VALUE}, new int[]{Integer.MAX_VALUE});
                        } else {
                            lf.fieldTextCreationYear.setRangeValues(new int[]{range[0]}, new int[]{range[1]});
                        }
                    }
                    if (lf.fieldTextPublishedYear != null) {
                        int[] range = cachedPublicationRanges.get(lang);
                        if (range == null) {
                            lf.fieldTextPublishedYear.setRangeValues(new int[]{Integer.MAX_VALUE}, new int[]{Integer.MAX_VALUE});
                        } else {
                            lf.fieldTextPublishedYear.setRangeValues(new int[]{range[0]}, new int[]{range[1]});
                        }
                    }
                } else {
                    sl.word.setData(EMPTY_STRINGARRAY);
                    sl.tags.setData(EMPTY_STRINGARRAY);
                    if (lf.fieldTextAuthor != null) sl.author.setData(EMPTY_STRINGARRAY);
                    if (lf.fieldTextSource != null) sl.source.setData(EMPTY_STRINGARRAY);
                    if (lf.fieldTextCreationYear != null) {
                        lf.fieldTextCreationYear.setRangeValues(new int[]{Integer.MAX_VALUE}, new int[]{Integer.MAX_VALUE});
                    }
                    if (lf.fieldTextPublishedYear != null) {
                        lf.fieldTextPublishedYear.setRangeValues(new int[]{Integer.MAX_VALUE}, new int[]{Integer.MAX_VALUE});
                    }
                }
            }

            fields.fieldSentencePBinary.setBytesValue(p.xml);

            if (globalIndexWriter != null) {
                try {
                    globalIndexWriter.addDocument(b.document);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    public static void finish() throws Exception {
        if (globalIndexWriter != null) {
            try {
                if (cacheForProduction) {
                    LOG.info("Force merging index...");
                    globalIndexWriter.forceMerge(1, true);
                }
                globalIndexWriter.close();
                globalDir.close();
                globalIndexWriter = null;
                globalDir = null;
            } catch (Throwable ex) {
                LOG.error("", ex);
                throw new Exception(ex);
            }
        }
    }
}
