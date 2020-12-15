package org.alex73.korpus.compiler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.Poetry;
import org.alex73.korpus.text.xml.Se;
import org.alex73.korpus.text.xml.W;

public class StatProcessing {
    private final Set<String> authors = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, StatInfo> stats = new HashMap<>();

    public void add(TextInfo textInfo, List<Object> content) {
        if (textInfo.authors != null) {
            for (String a : textInfo.authors) {
                authors.add(a);
            }
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

        content.parallelStream().forEach(p -> {
            if (p instanceof P) {
                process((P) p, todo);
            } else if (p instanceof Poetry) {
                Poetry po = (Poetry) p;
                for (Object p2 : po.getPOrTag()) {
                    if (p2 instanceof P) {
                        process((P) p2, todo);
                    }
                }
            }
        });
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
            en.getValue().write(dir.resolve("stat.freq." + en.getKey().replace('/', '_') + ".tab"));
        }

        List<String> stat = new ArrayList<>();
        stat.add("authors=" + String.join(";", authors));
        for (Map.Entry<String, StatInfo> en : stats.entrySet()) {
            stat.add("texts." + en.getKey() + "=" + en.getValue().texts);
            stat.add("words." + en.getKey() + "=" + en.getValue().words);
        }
        Files.write(dir.resolve("stat.properties"), stat);
    }

    private static final Pattern RE_SPLIT = Pattern.compile("_");

    private void process(P p, List<StatInfo> todo) {
        for (Se se : p.getSe()) {
            for (Object o : se.getWOrSOrZ()) {
                if (o instanceof W) {
                    W w = (W) o;
                    if (w.getLemma() != null) {
                        String[] lemmas = RE_SPLIT.split(w.getLemma());
                        todo.forEach(s -> s.addWord(w.getValue(), lemmas));
                    }
                }
            }
        }
    }

    private static class StatInfo {
        public int texts, words;
        private final Map<String, ParadigmStat> byLemma = new HashMap<>();

        synchronized void addWord(String value, String[] lemmas) {
            words++;
            float part = 1.0f / lemmas.length;
            for (String lemma : lemmas) {
                if (lemma.isEmpty()) {
                    continue;
                }
                ParadigmStat ps;
                synchronized (this) {
                    ps = byLemma.get(lemma);
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

        void write(Path f) throws Exception {
            List<ParadigmStat> list = byLemma.values().stream()
                    .sorted((a, b) -> Integer.compare(b.intCount, a.intCount)).collect(Collectors.toList());
            Files.write(f, list.stream().map(s -> s.para + "\t" + s.intCount + "\t" + s.valuesCount)
                    .collect(Collectors.toList()));
        }
    }
    
    static class ParadigmStat {
        String para;
        int intCount;
        float floatCount;
        Map<String,Integer> valuesCount=new TreeMap<>();
    }
}
