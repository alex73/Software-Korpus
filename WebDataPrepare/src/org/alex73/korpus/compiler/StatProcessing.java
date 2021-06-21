package org.alex73.korpus.compiler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.text.structure.corpus.Word;
import org.alex73.korpus.utils.StressUtils;

public class StatProcessing {
    private final Map<String, StatInfo> stats = new HashMap<>();

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
        if (textInfo.styleGenres != null && textInfo.styleGenres.length > 0) {
            for (String s : textInfo.styleGenres) {
                todo.add(getStatInfo(textInfo.subcorpus + "." + s));
            }
        } else {
            todo.add(getStatInfo(textInfo.subcorpus + "._"));
        }
        todo.forEach(s -> {
            synchronized (s) {
                s.texts++;
            }
        });

        for (Paragraph p : content) {
            for (Sentence se : p.sentences) {
                for (Word w : se.words) {
                    String[] lemmas = w.lemmas == null || w.lemmas.isEmpty() ? null : RE_SPLIT.split(w.lemmas);
                    todo.forEach(s -> s.addWord(w.normalized, lemmas));
                }
            }
        }
    }

    private synchronized StatInfo getStatInfo(String key) {
        StatInfo r = stats.get(key);
        if (r == null) {
            r = new StatInfo();
            stats.put(key, r);
        }
        return r;
    }

    public synchronized void write(Path dir) throws Exception {
        for (Map.Entry<String, StatInfo> en : stats.entrySet()) {
            en.getValue().writeFormsFreq(dir.resolve("stat.formsfreq." + en.getKey().replace('/', '_') + ".tab"));
            en.getValue().writeLemmasFreq(dir.resolve("stat.lemmasfreq." + en.getKey().replace('/', '_') + ".tab"));
            en.getValue().writeUnknownFreq(dir.resolve("stat.unknownfreq." + en.getKey().replace('/', '_') + ".tab"));
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
            stat.add("texts." + en.getKey() + "=" + en.getValue().texts);
            stat.add("words." + en.getKey() + "=" + en.getValue().words);
        }
        Files.write(dir.resolve("stat.properties"), stat);
    }

    private static final Collator BE = Collator.getInstance(new Locale("be"));

    private String joinSorted(Collection<String> list) {
        return list.stream().sorted(BE).collect(Collectors.joining(";"));
    }

    private static final Pattern RE_SPLIT = Pattern.compile(";");

    private static class StatInfo {
        public int texts, words;
        private final Map<String, ParadigmStat> byLemma = new HashMap<>();
        private final Map<String, Integer> byForm = new HashMap<>();
        private final Map<String, Integer> byUnknown = new HashMap<>();
        final Set<String> authors = Collections.synchronizedSet(new HashSet<>());
        final Set<String> sources = Collections.synchronizedSet(new HashSet<>());

        synchronized void addWord(String value, String[] lemmas) {
            words++;
            String formKey = StressUtils.unstress(value).toLowerCase();
            if (!formKey.isEmpty()) {
                if (formKey.charAt(0) == 'ў') {
                    formKey = 'у' + formKey.substring(1);
                }
                Integer fc = byForm.get(formKey);
                fc = fc == null ? 1 : fc.intValue() + 1;
                byForm.put(formKey, fc);
            }
            if (lemmas == null) {
                // TODO адкидаць пустыя словы, нумары, лацінку, 
                if (formKey.isEmpty()) {
                    return;
                }
                for (int i = 0; i < formKey.length(); i++) {
                    char c = formKey.charAt(i);
                    if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')) {
                        return;
                    }
                }
                Integer fc = byUnknown.get(formKey);
                fc = fc == null ? 1 : fc.intValue() + 1;
                byUnknown.put(formKey, fc);
            } else {
                float part = 1.0f / lemmas.length;
                for (String lemma : lemmas) {
                    if (lemma.isEmpty()) {
                        continue;
                    }
                    ParadigmStat ps = byLemma.get(lemma);
                    if (ps == null) {
                        ps = new ParadigmStat();
                        ps.para = lemma;
                        byLemma.put(lemma, ps);
                    }
                    ps.intCount++;
                    ps.floatCount += part;
                    Integer prev = ps.valuesCount.get(value);
                    ps.valuesCount.put(value, prev == null ? 1 : prev.intValue() + 1);
                }
            }
        }

        void writeFormsFreq(Path f) throws Exception {
            List<String> list = byForm.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .map(en -> en.toString()).collect(Collectors.toList());
            Files.write(f, list);
        }

        void writeUnknownFreq(Path f) throws Exception {
            List<String> list = byUnknown.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).map(en -> en.toString())
                    .collect(Collectors.toList());
            Files.write(f, list);
        }

        void writeLemmasFreq(Path f) throws Exception {
            List<String> list = byLemma.values().stream().sorted((a, b) -> Integer.compare(b.intCount, a.intCount))
                    .map(s -> s.para + "\t" + s.intCount + "\t" + s.valuesCount).collect(Collectors.toList());
            Files.write(f, list);
        }
    }

    static class ParadigmStat {
        String para;
        int intCount;
        float floatCount;
        Map<String, Integer> valuesCount = new TreeMap<>();
    }
}
