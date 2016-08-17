package org.alex73.korpus.base;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.korpus.utils.StressUtils;
import org.alex73.korpus.utils.WordNormalizer;

public class GrammarParadigmFinder {
    private Map<String, Paradigm[]> paradigmsByForm = new HashMap<>();

    public GrammarParadigmFinder(GrammarDB2 gr) {
        gr.getAllParadigms().parallelStream().forEach(p -> {
            p.getVariant().forEach(v -> {
                v.getForm().forEach(f -> {
                    String orig = WordNormalizer.normalize(f.getValue());
                    add(orig, p);
                    String s = StressUtils.unstress(orig);
                    if (!s.equals(orig)) {
                        add(s, p);
                    }
                });
            });
        });
    }

    private synchronized void add(String key, Paradigm p) {
        Paradigm[] byForm = paradigmsByForm.get(key);
        if (byForm == null) {
            byForm = new Paradigm[1];
        } else {
            if (byForm[byForm.length - 1] == p) {
                // already stored
                return;
            }
            byForm = Arrays.copyOf(byForm, byForm.length + 1);
        }
        byForm[byForm.length - 1] = p;
        paradigmsByForm.put(key, byForm);
    }

    /**
     * Find paradigms by word (unstressed, lower case).
     */
    public Paradigm[] getParadigmsByForm(String word) {
        if (word.startsWith("ў")) {
            word = 'у' + word.substring(1);
        } else if (word.startsWith("Ў")) {
            word = 'У' + word.substring(1);
        }
        Paradigm[] r = paradigmsByForm.get(word);
        if (r == null) {
            r = paradigmsByForm.get(word.replace("*", ""));
        }
        if (r == null) {
            r = paradigmsByForm.get(word.toLowerCase());
        }
        if (r == null) {
            r = paradigmsByForm.get(word.replace("*", "").toLowerCase());
        }
        return r;
    }
}
