package org.alex73.korpus.compiler;

import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.text.structure.corpus.Word;
import org.alex73.korpus.utils.KorpusFileUtils;
import org.alex73.korpus.utils.StressUtils;

public class StatProcessing {
    // stat by subcorpuses,styles,genres
    private final Map<String, StatInfo> stats = new HashMap<>();
    // authors by lemmas
    private final Map<String, Set<String>> authorsByLemmas = new HashMap<>();

    public StatProcessing() {
        stats.put("", new StatInfo(true));
        stats.put("teksty", new StatInfo(true));
        stats.put("skaryna", new StatInfo(true));
        stats.put("kankardans", new StatInfo(true));
        stats.put("dyjalektny", new StatInfo(true));
    }

    public void add(TextInfo textInfo, List<Paragraph> content) {
        StatInfo subcorpusInfo = getStatInfo(textInfo.subcorpus);
        if (textInfo.authors != null) {
            for (String a : textInfo.authors) {
                subcorpusInfo.authors.add(a);
            }
        }
        if (textInfo.source != null) {
            subcorpusInfo.sources.add(textInfo.source);
        }

        List<StatInfo> todo = new ArrayList<>();
        todo.add(getStatInfo(""));
        todo.add(getStatInfo(textInfo.subcorpus));
        switch (textInfo.subcorpus) {
        case "teksty":
            if (textInfo.styleGenres != null) {
                Arrays.asList(textInfo.styleGenres).stream().map(s -> s.replaceAll("/.+", "")).sorted().distinct()
                        .forEach(s -> todo.add(getStatInfo(textInfo.subcorpus + "." + s)));
            }
            break;
        case "sajty":
        case "nierazabranaje":
            todo.add(getStatInfo(textInfo.subcorpus + "." + textInfo.source));
            break;
        }
        todo.forEach(s -> s.texts.incrementAndGet());

        for (Paragraph p : content) {
            for (Sentence se : p.sentences) {
                for (Word w : se.words) {
                    String[] lemmas = w.lemmas == null || w.lemmas.isEmpty() ? null : RE_SPLIT.split(w.lemmas);
                    todo.forEach(s -> s.addWord(w.normalized, lemmas));
                    if (lemmas != null && textInfo.authors != null && textInfo.authors.length > 0) {
                        for (String lemma : lemmas) {
                            for (String author : textInfo.authors) {
                                addAuthorByLemma(lemma, author);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addAuthorByLemma(String lemma, String author) {
        Set<String> authors;
        synchronized (authorsByLemmas) {
            authors = authorsByLemmas.get(lemma);
            if (authors == null) {
                authors = new HashSet<>();
                authorsByLemmas.put(lemma, authors);
            }
        }
        synchronized (authors) {
            authors.add(author);
        }
    }

    private synchronized StatInfo getStatInfo(String key) {
        return stats.computeIfAbsent(key, k -> new StatInfo(false));
    }

    public synchronized void write(Path dir) throws Exception {
        KorpusFileUtils.writeGzip(dir.resolve("stat.formsfreq.tab.gz"), stats.get("").getFormsFreq());

        try (ZipOutputStream zip = new ZipOutputStream(
                new BufferedOutputStream(Files.newOutputStream(dir.resolve("stat-freq.zip"))))) {
            for (Map.Entry<String, StatInfo> en : stats.entrySet()) {
                en.getValue().writeFormsFreq(zip,
                        ("forms/freq." + en.getKey().replace('/', '_') + ".tab").replace("..", "."));
                en.getValue().writeLemmasFreq(zip,
                        ("lemmas/freq." + en.getKey().replace('/', '_') + ".tab").replace("..", "."));
                en.getValue().writeUnknownFreq(zip,
                        ("unknown/freq." + en.getKey().replace('/', '_') + ".tab").replace("..", "."));
            }
        }

        List<String> stat = new ArrayList<>();
        for (Map.Entry<String, StatInfo> en : stats.entrySet()) {
            if (!en.getValue().authors.isEmpty()) {
                stat.add("authors." + en.getKey() + "=" + joinSorted(en.getValue().authors));
            }
            if (!en.getValue().sources.isEmpty()) {
                stat.add("sources." + en.getKey() + "=" + joinSorted(en.getValue().sources));
            }
        }
        for (Map.Entry<String, StatInfo> en : stats.entrySet()) {
            stat.add("texts." + en.getKey() + "=" + en.getValue().texts.intValue());
            stat.add("words." + en.getKey() + "=" + en.getValue().words.intValue());
        }
        Files.write(dir.resolve("stat.properties"), stat);

        List<String> lemmas = new ArrayList<>(authorsByLemmas.keySet());
        Collections.sort(lemmas, BE);

        KorpusFileUtils.writeGzip(dir.resolve("lemma-authors.list.gz"),
                lemmas.stream().map(le -> le + '=' + String.join(";", authorsByLemmas.get(le))));
    }

    private static final Collator BE = Collator.getInstance(new Locale("be"));

    private String joinSorted(Collection<String> list) {
        return list.stream().sorted(BE).collect(Collectors.joining(";"));
    }

    private static final Pattern RE_SPLIT = Pattern.compile(";");

    private static class StatInfo {
        public AtomicInteger texts = new AtomicInteger();
        public AtomicInteger words = new AtomicInteger();
        private Map<String, ParadigmStat> byLemma = null; // TODO disabled stat new HashMap<>();
        private Map<String, AtomicInteger> byForm = null;
        private Map<String, AtomicInteger> byUnknown = null; /*
                                                              * new HashMapSyncInit<>() {
                                                              * 
                                                              * @Override public AtomicInteger init() { return new AtomicInteger(); } };
                                                              */
        Set<String> authors = Collections.synchronizedSet(new HashSet<>());
        Set<String> sources = Collections.synchronizedSet(new HashSet<>());

        public StatInfo(boolean collectForms) {
            if (collectForms) {
                byForm = new HashMapSyncInit<>() {
                    @Override
                    public AtomicInteger init() {
                        return new AtomicInteger();
                    }
                };
            }
        }

        void addWord(String value, String[] lemmas) {
            words.incrementAndGet();

            String normalizedValue = StressUtils.unstress(value).toLowerCase();
            if (!normalizedValue.isEmpty()) {
                if (normalizedValue.charAt(0) == 'ў') {
                    normalizedValue = 'у' + normalizedValue.substring(1);
                }
            }

            if (byForm != null && !normalizedValue.isEmpty()) {
                byForm.get(normalizedValue).incrementAndGet();
            }
            if (lemmas == null) {
                if (byUnknown != null) {
                    // TODO адкідаць пустыя словы, нумары, лацінку,
                    if (normalizedValue.isEmpty()) {
                        return;
                    }
                    for (int i = 0; i < normalizedValue.length(); i++) {
                        char c = normalizedValue.charAt(i);
                        if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')) {
                            return;
                        }
                    }
                    byUnknown.get(normalizedValue).incrementAndGet();
                }
            } else {
                if (byLemma != null) {
                    float part = 1.0f / lemmas.length;
                    for (String lemma : lemmas) {
                        if (lemma.isEmpty()) {
                            continue;
                        }
                        ParadigmStat ps;
                        synchronized (byLemma) {
                            ps = byLemma.get(lemma);
                            if (ps == null) {
                                ps = new ParadigmStat();
                                ps.para = lemma;
                                byLemma.put(lemma, ps);
                            }
                        }
                        synchronized (ps) {
                            ps.intCount++;
                            ps.floatCount += part;
                            Integer prev = ps.valuesCount.get(normalizedValue);
                            ps.valuesCount.put(normalizedValue, prev == null ? 1 : prev.intValue() + 1);
                        }
                    }
                }
            }
        }

        synchronized Stream<String> getFormsFreq() throws Exception {
            if (byForm != null) {
                return byForm.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue().get(), a.getValue().get())).map(en -> en.toString());
            } else {
                return Stream.empty();
            }
        }

        synchronized void writeFormsFreq(ZipOutputStream zip, String entryName) throws Exception {
            if (byForm != null) {
                KorpusFileUtils.writeZip(zip, entryName,
                        byForm.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue().get(), a.getValue().get())).map(en -> en.toString()));
            }
        }

        synchronized void writeUnknownFreq(ZipOutputStream zip, String entryName) throws Exception {
            if (byUnknown != null) {
                KorpusFileUtils.writeZip(zip, entryName,
                        byUnknown.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue().get(), a.getValue().get())).map(en -> en.toString()));
            }
        }

        synchronized void writeLemmasFreq(ZipOutputStream zip, String entryName) throws Exception {
            if (byLemma != null) {
                KorpusFileUtils.writeZip(zip, entryName, byLemma.values().stream().sorted((a, b) -> Integer.compare(b.intCount, a.intCount))
                        .map(s -> s.para + "\t" + s.intCount + "\t" + s.valuesCount));
            }
        }
    }

    static class ParadigmStat {
        String para;
        int intCount;
        float floatCount;
        Map<String, Integer> valuesCount = new TreeMap<>();
    }

    public abstract static class HashMapSyncInit<K, V> extends HashMap<K, V> {
        private ReadWriteLock lock = new ReentrantReadWriteLock();
        private Lock rd = lock.readLock();
        private Lock wr = lock.writeLock();

        public abstract V init();

        @Override
        public V get(Object key) {
            rd.lock();
            V result = super.get(key);
            rd.unlock();
            if (result == null) {
                wr.lock();
                result = super.get(key);
                if (result == null) {
                    result = init();
                    super.put((K) key, (V) result);
                }
                wr.unlock();
            }
            return result;
        }

        public synchronized V oldget(Object key) {
            V result = super.get(key);
            if (result == null) {
                result = init();
                super.put((K) key, (V) result);
            }
            return result;
        }
    }
}
