package org.alex73.korpus.compiler;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.Poetry;
import org.alex73.korpus.text.xml.Se;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.XMLText;

public class StatProcessing {
    public static final Map<String, Integer> countsByOne = new HashMap<>();
    public static final Map<String, Float> countsByPart = new HashMap<>();

    public static void add(XMLText doc) {
        for (Object p : doc.getContent().getPOrTagOrPoetry()) {
            if (p instanceof P) {
                process((P) p);
            } else if (p instanceof Poetry) {
                Poetry po = (Poetry) p;
                for (Object p2 : po.getPOrTag()) {
                    if (p2 instanceof P) {
                        process((P) p2);
                    }
                }
            }
        }
    }

    public static void write(String dir) throws Exception {
        List<String> listByOne = new ArrayList<>(countsByOne.keySet());
        Collections.sort(listByOne, COMP_BY_ONE);
        write(listByOne, countsByOne, dir + "/statWords.tab");
    }

    static void write(List<String> keys, Map<String, ?> values, String fn) throws Exception {
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            keys.set(i, k + '\t' + values.get(k));
        }
        Files.write(Paths.get(fn), keys);
    }

    static final Pattern RE_SPLIT = Pattern.compile("_");

    static void process(P p) {
        for (Se se : p.getSe()) {
            for (Object o : se.getWOrSOrZ()) {
                if (o instanceof W) {
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
    }

    static synchronized void inc(String lemma, float part) {
        Integer prev1 = countsByOne.get(lemma);
        int v1 = prev1 == null ? 1 : prev1 + 1;
        countsByOne.put(lemma, v1);
        Float prev2 = countsByPart.get(lemma);
        float v2 = prev2 == null ? part : prev2 + part;
        countsByPart.put(lemma, v2);
    }

    static final Comparator<String> COMP_BY_ONE = new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
            int v1 = countsByOne.get(s1);
            int v2 = countsByOne.get(s2);
            return Integer.compare(v2, v1);
        }
    };
    static final Comparator<String> COMP_BY_PART = new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
            float v1 = countsByPart.get(s1);
            float v2 = countsByPart.get(s2);
            return Float.compare(v2, v1);
        }
    };
}
