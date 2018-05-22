package org.alex73.korpus.compiler;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.Poetry;
import org.alex73.korpus.text.xml.Se;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.XMLText;

public class StatProcessing {
    private final Map<String, Integer> countsByOne = new HashMap<>();
    private final Map<String, Float> countsByPart = new HashMap<>();
    private final Set<String> authors = new TreeSet<>();
    private final StatInfo total = new StatInfo("");
    private final Map<String, StatInfo> parts = new HashMap<>();
    private final Set<String> volumes = new TreeSet<>();

    public void add(TextInfo textInfo, XMLText doc) {
        int wc = 0;
        for (Object p : doc.getContent().getPOrTagOrPoetry()) {
            if (p instanceof P) {
                wc += process((P) p);
            } else if (p instanceof Poetry) {
                Poetry po = (Poetry) p;
                for (Object p2 : po.getPOrTag()) {
                    if (p2 instanceof P) {
                        wc += process((P) p2);
                    }
                }
            }
        }

        synchronized (this) {
            total.addText(wc);
            if (textInfo.styleGenres.length > 0) {
                for (String s : textInfo.styleGenres) {
                    addKorpusStat(s, wc);
                }
            } else {
                addKorpusStat("_", wc);
            }
            for (String a : textInfo.authors) {
                authors.add(a);
            }
        }
    }

    public synchronized void addVolume(String v) {
        volumes.add(v);
    }

    private void addKorpusStat(String styleGenre, int wordsCount) {
        int p = styleGenre.indexOf('/');
        String st = p < 0 ? styleGenre : styleGenre.substring(0, p);
        StatInfo i = parts.get(st);
        if (i == null) {
            i = new StatInfo(st);
            parts.put(st, i);
        }
        i.addText(wordsCount);
    }

    public synchronized void write(String dir) throws Exception {
        List<String> listByOne = new ArrayList<>(countsByOne.keySet());
        Collections.sort(listByOne, COMP_BY_ONE);
        write(listByOne, countsByOne, dir + "/statWords.tab");

        Properties stat = new Properties();

        String authorsstr = "";
        for (String s : authors) {
            authorsstr += ";" + s;
        }
        if (!authors.isEmpty()) {
            stat.setProperty("authors", authorsstr.substring(1));
        } else {
            stat.setProperty("authors", "");
        }
        for (StatInfo si : parts.values()) {
            si.write(stat); // - don't output details yet
        }
        total.write(stat);

        String volumesstr = "";
        for (String s : volumes) {
            volumesstr += ";" + s;
        }
        if (!volumesstr.isEmpty()) {
            stat.setProperty("volumes", volumesstr.substring(1));
        } else {
            stat.setProperty("volumes", "");
        }

        try (FileOutputStream o = new FileOutputStream(dir + "/stat.properties")) {
            stat.store(o, null);
        }
    }

    private void write(List<String> keys, Map<String, ?> values, String fn) throws Exception {
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            keys.set(i, k + '\t' + values.get(k));
        }
        Files.write(Paths.get(fn), keys);
    }

    private static final Pattern RE_SPLIT = Pattern.compile("_");

    private int process(P p) {
        int wc = 0;
        for (Se se : p.getSe()) {
            for (Object o : se.getWOrSOrZ()) {
                if (o instanceof W) {
                    wc++;
                    W w = (W) o;
                    if (w.getLemma() != null) {
                        String[] lemmas = RE_SPLIT.split(w.getLemma());
                        float part = 1.0f / lemmas.length;
                        for (String lemma : lemmas) {
                            inc(lemma, part);
                        }
                    }
                }
            }
        }
        return wc;
    }

    private synchronized void inc(String lemma, float part) {
        Integer prev1 = countsByOne.get(lemma);
        int v1 = prev1 == null ? 1 : prev1 + 1;
        countsByOne.put(lemma, v1);
        Float prev2 = countsByPart.get(lemma);
        float v2 = prev2 == null ? part : prev2 + part;
        countsByPart.put(lemma, v2);
    }

    private final Comparator<String> COMP_BY_ONE = new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
            int v1 = countsByOne.get(s1);
            int v2 = countsByOne.get(s2);
            return Integer.compare(v2, v1);
        }
    };
    private final Comparator<String> COMP_BY_PART = new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
            float v1 = countsByPart.get(s1);
            float v2 = countsByPart.get(s2);
            return Float.compare(v2, v1);
        }
    };

    private static class StatInfo {
        private final String suffix;
        public int texts, words;

        public StatInfo(String suffix) {
            this.suffix = suffix.isEmpty() ? suffix : "." + suffix;
        }

        public void addText(int wordsCount) {
            texts++;
            words += wordsCount;
        }

        public void write(Properties props) {
            props.setProperty("texts" + suffix, "" + texts);
            props.setProperty("words" + suffix, "" + words);
        }
    }
}
