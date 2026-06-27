package org.alex73.korpus.compiler;

import com.google.gson.Gson;
import org.alex73.grammardb.GrammarFinder;
import org.alex73.grammardb.StressUtils;
import org.alex73.grammardb.structures.Form;
import org.alex73.grammardb.structures.Paradigm;
import org.alex73.grammardb.structures.Variant;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.stat.StatWriting;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.text.structure.corpus.Word;
import org.alex73.korpus.utils.KorpusFileUtils;
import org.rocksdb.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Трэці этап кампіляцыі корпуса.
 * Клас для збору і апрацоўкі статыстычных даных: падлік частотнасці слоў і лем,
 * фармаванне спісаў аўтараў па лемах і генерацыя справаздач па падкорпусах.
 */
public class Step3Stat {
    public static final int MAX_STAT_IN_MEMORY = 100000;

    private static final String LANG = "bel";

    private static GrammarFinder grFinder;
    private static Path tempOutputDir;
    // stat by subcorpuses
    private static Map<String, TextStatInfo> textStatInfos = new ConcurrentHashMap<>();
    // authors by lemmas: Map<lemma, Set<author>>
    private static Map<String, Set<String>> authorsByLemmas = new ConcurrentHashMap<>();

    private static RocksDB statDb;
    private static Path dbPath;
    private static Set<String> knownSubcorpuses = Collections.synchronizedSet(new TreeSet<>());

    public static void init(Path tempDir, GrammarFinder finder) throws Exception {
        tempOutputDir = tempDir;
        grFinder = finder;
        dbPath = tempOutputDir.resolve("stat_rocksdb");

        RocksDB.destroyDB(dbPath.toString(), new Options().setCreateIfMissing(true));

        Options options = new Options()
                .setCreateIfMissing(true)
                .setMergeOperator(new UInt64AddOperator())
                .setUnorderedWrite(true)
                .setCompressionType(CompressionType.LZ4_COMPRESSION)
                .setErrorIfExists(true)
                .setMaxBackgroundJobs(6)
                .setBytesPerSync(1048576)
                .setWriteBufferSize(256 * 1024 * 1024)
                .setMaxWriteBufferNumber(4)
                .setMinWriteBufferNumberToMerge(2)
                .setLevel0FileNumCompactionTrigger(4)
                .setDelayedWriteRate(128L * 1024 * 1024)
                .setTargetFileSizeBase(64 * 1024 * 1024)
                .setIncreaseParallelism(Runtime.getRuntime().availableProcessors());
        statDb = RocksDB.open(options, dbPath.toString());
    }

    public static void run(MessageParsedText text) throws Exception {
        for (int i = 0; i < text.textInfo.subtexts.length; i++) {
            if (!LANG.equals(text.languages[i].lang)) {
                continue;
            }
            TextStatLocal stat = calcWordsStat(text, i);
            String subcorpus = text.textInfo.subcorpus;
            knownSubcorpuses.add(subcorpus);

            byte[] subcorpusPrefix = (subcorpus + "\0").getBytes(UTF_8);
            byte[] globalPrefix = "\0".getBytes(UTF_8);

            try (WriteBatch batch = new WriteBatch(); WriteOptions writeOpts = new WriteOptions()) {
                writeOpts.setDisableWAL(true);
                for (Map.Entry<String, Integer> en : stat.wordCounts.entrySet()) {
                    byte[] wordBytes = en.getKey().getBytes(UTF_8);
                    byte[] val = longToLittleEndian(en.getValue());

                    byte[] subKey = new byte[subcorpusPrefix.length + wordBytes.length];
                    System.arraycopy(subcorpusPrefix, 0, subKey, 0, subcorpusPrefix.length);
                    System.arraycopy(wordBytes, 0, subKey, subcorpusPrefix.length, wordBytes.length);
                    batch.merge(subKey, val);
                }
                statDb.write(writeOpts, batch);
                batch.clear();
            }
            int totalWords = stat.wordCounts.values().stream().mapToInt(Integer::intValue).sum();
            addGlobalCounts(text.textInfo, i, totalWords);
            if (text.textInfo.subtexts[i].authors != null) {
                for (String author : text.textInfo.subtexts[i].authors) {
                    for (String lemma : stat.lemmas) {
                        addAuthorByLemma(lemma, author);
                    }
                }
            }
        }
    }

    static class TextStatLocal {
        Map<String, Integer> wordCounts = new HashMap<>();
        Set<String> lemmas = new HashSet<>();
    }

    private static final Map<String, Set<String>> lemmaCache = new ConcurrentHashMap<>();

    private static TextStatLocal calcWordsStat(MessageParsedText text, int textIndex) {
        TextStatLocal stat = new TextStatLocal();
        for (Paragraph p : text.languages[textIndex].paragraphs) {
            for (Sentence se : p.sentences) {
                for (Word w : se.words) {
                    if (w.wordNormalized != null) {
                        stat.wordCounts.merge(w.wordNormalized, 1, Integer::sum);
                        stat.lemmas.addAll(lemmaCache.computeIfAbsent(w.wordNormalized, Step3Stat::getLemmasByForm));
                    }
                }
            }
        }
        return stat;
    }

    private static Set<String> getLemmasByForm(String form) {
        Set<String> result = new TreeSet<>();
        for (Paradigm p : grFinder.getParadigms(form)) {
            for (Variant v : p.getVariant()) {
                for (Form f : v.getForm()) {
                    if (fastCompareWithoutStress(f.getValue(), form)) {
                        result.add(p.getLemma());
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static boolean fastCompareWithoutStress(String s1, String s2) {
        int p1 = 0, p2 = 0;
        while (true) {
            char c1 = p1 < s1.length() ? s1.charAt(p1) : 0;
            char c2 = p2 < s2.length() ? s2.charAt(p2) : 0;
            if (c1 == 0 && c2 == 0) {
                return true;
            }
            if (StressUtils.STRESS_CHARS.indexOf(c1) >= 0) {
                p1++;
                continue;
            }
            if (StressUtils.STRESS_CHARS.indexOf(c2) >= 0) {
                p2++;
                continue;
            }
            if (Character.isUpperCase(c1)) {
                c1 = Character.toLowerCase(c1);
            }
            if (Character.isUpperCase(c2)) {
                c2 = Character.toLowerCase(c2);
            }
            if (c1 != c2) {
                return false;
            }
            p1++;
            p2++;
        }
    }

    private static void addGlobalCounts(TextInfo textInfo, int textIndex, int wordsCount) {
        TextStatInfo commonStatInfo = getTextStatInfo("");
        commonStatInfo.texts.incrementAndGet();
        commonStatInfo.words.addAndGet(wordsCount);
        TextStatInfo subcorpusStatInfo = getTextStatInfo(textInfo.subcorpus);
        subcorpusStatInfo.texts.incrementAndGet();
        subcorpusStatInfo.words.addAndGet(wordsCount);
        if (textInfo.subtexts[textIndex].authors != null) {
            for (String a : textInfo.subtexts[textIndex].authors) {
                subcorpusStatInfo.authors.add(a);
            }
        }
        if (textInfo.subtexts[textIndex].sourceName != null) {
            subcorpusStatInfo.sources.add(textInfo.subtexts[textIndex].sourceName);
        }
        if (textInfo.styleGenres != null) {
            Arrays.asList(textInfo.styleGenres).stream().map(s -> s.replaceAll("/.+", "")).sorted().distinct().forEach(s -> {
                TextStatInfo subcorpusStyleGenre = getTextStatInfo(textInfo.subcorpus + "." + s);
                subcorpusStyleGenre.texts.incrementAndGet();
                subcorpusStyleGenre.words.addAndGet(wordsCount);
            });
        }
        if (textInfo.subtexts[textIndex].sourceName != null) {
            TextStatInfo subcorpusSource = getTextStatInfo(textInfo.subcorpus + "." + textInfo.subtexts[textIndex].sourceName);
            subcorpusSource.texts.incrementAndGet();
            subcorpusSource.words.addAndGet(wordsCount);
        }
    }

    public static void flush() throws Exception {
    }

    public static void finish(Path outputDir, RocksDB outputTextsDb, ColumnFamilyHandle authorsByLemmasCf) throws Exception {
        StatWriting.write(textStatInfos, outputDir);
        try (WriteOptions writeOptions = new WriteOptions()) {
            writeOptions.setDisableWAL(true);
            Gson gson = new Gson();
            for (Map.Entry<String, Set<String>> e : authorsByLemmas.entrySet()) {
                outputTextsDb.put(authorsByLemmasCf, writeOptions, e.getKey().getBytes(UTF_8), gson.toJson(e.getValue()).getBytes(UTF_8));
            }
        }
    }

    public static void mergeToZip(ZipOutputStream zip) throws Exception {
        List<Runnable> runs = new ArrayList<>();

        // Агульная мапа для ўсіх слоў са ўсіх сабкорпусаў
        Map<String, AtomicInteger> globalWordCounts = new ConcurrentHashMap<>();

        for (String subcorpus : knownSubcorpuses) {
            runs.add(() -> mergeAndWrite(zip, "forms/freq." + subcorpus + ".tab", subcorpus, globalWordCounts));
        }

        runs.stream().forEach(Runnable::run);

        synchronized (zip) {
            KorpusFileUtils.writeZip(zip, "forms/freq.tab",
                    globalWordCounts.entrySet().stream()
                            .sorted((a, b) -> Integer.compare(b.getValue().get(), a.getValue().get()))
                            .map(en -> en.getKey() + "=" + en.getValue().get()));
        }
    }

    static class MutableInt {
        int value;

        MutableInt(int v) {
            value = v;
        }
    }

    private static void mergeAndWrite(ZipOutputStream out, String entryName, String prefixStr, Map<String, AtomicInteger> globalWordCounts) {
        try {
            Map<String, MutableInt> wordCounts = new HashMap<>();
            byte[] prefix = (prefixStr + "\0").getBytes(UTF_8);
            try (RocksIterator iter = statDb.newIterator()) {
                iter.seek(prefix);
                while (iter.isValid()) {
                    byte[] key = iter.key();
                    if (key.length < prefix.length) break;
                    boolean match = true;
                    for (int i = 0; i < prefix.length; i++) {
                        if (key[i] != prefix[i]) {
                            match = false;
                            break;
                        }
                    }
                    if (!match) break;

                    String word = new String(key, prefix.length, key.length - prefix.length, UTF_8);
                    long count = ByteBuffer.wrap(iter.value()).order(ByteOrder.LITTLE_ENDIAN).getLong();
                    wordCounts.put(word, new MutableInt((int) count));
                    globalWordCounts.computeIfAbsent(word, w -> new AtomicInteger(0)).addAndGet((int) count);
                    iter.next();
                }
            }
            synchronized (out) {
                KorpusFileUtils.writeZip(out, entryName,
                        wordCounts.entrySet().stream()
                                .sorted((a, b) -> Integer.compare(b.getValue().value, a.getValue().value))
                                .map(en -> en.getKey() + "=" + en.getValue().value));
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void removeTemp() throws IOException {
        if (statDb != null) {
            statDb.close();
            statDb = null;
        }
    }

    private static void addAuthorByLemma(String lemma, String author) {
        authorsByLemmas.computeIfAbsent(lemma, l -> ConcurrentHashMap.newKeySet()).add(author);
    }

    private static TextStatInfo getTextStatInfo(String key) {
        return textStatInfos.computeIfAbsent(key, k -> new TextStatInfo());
    }

    public static byte[] longToLittleEndian(long value) {
        return new byte[]{
                (byte) value,
                (byte) (value >> 8),
                (byte) (value >> 16),
                (byte) (value >> 24),
                (byte) (value >> 32),
                (byte) (value >> 40),
                (byte) (value >> 48),
                (byte) (value >> 56)
        };
    }

    public static class TextStatInfo {
        public AtomicLong texts = new AtomicLong();
        public AtomicLong words = new AtomicLong();
        public Set<String> authors = Collections.synchronizedSet(new HashSet<>());
        public Set<String> sources = Collections.synchronizedSet(new HashSet<>());
    }
}
