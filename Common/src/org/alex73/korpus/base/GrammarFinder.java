package org.alex73.korpus.base;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.korpus.utils.StressUtils;

public class GrammarFinder implements IGrammarFinder {
    private Map<String, Paradigm[]> paradigmsByForm;

    public GrammarFinder(GrammarDB2 gr) {
        paradigmsByForm = new HashMap<>(gr.getAllParadigms().size());
        long be = System.currentTimeMillis();
        gr.getAllParadigms().parallelStream().forEach(p -> {
            p.getVariant().forEach(v -> {
                v.getForm().forEach(f -> {
                    String orig = BelarusianWordNormalizer.normalize(f.getValue());
                    add(orig, p);
                    String s = StressUtils.unstress(orig);
                    if (!s.equals(orig)) {
                        add(s, p);
                    }
                });
            });
        });
        long af = System.currentTimeMillis();
        System.out.println("GrammarFinder prepare time: " + (af - be) + "ms");
    }

    /**
     * Must be synchronized because executed in the many thread by constructor.
     */
    private synchronized void add(String key, Paradigm p) {
        Paradigm[] byForm = paradigmsByForm.get(key);
        if (byForm == null) {
            byForm = new Paradigm[1];
        } else {
            for (int i = byForm.length - 1; i >= 0; i--) {
                if (byForm[i] == p) {
                    return;
                }
            }
            byForm = Arrays.copyOf(byForm, byForm.length + 1);
        }
        byForm[byForm.length - 1] = p;
        paradigmsByForm.put(key, byForm);
    }

    /**
     * Find paradigms by word (lower case).
     */
    public Paradigm[] getParadigmsByForm(String word) {
        word = BelarusianWordNormalizer.normalize(word);
        Paradigm[] r = paradigmsByForm.get(word);
        if (r == null) {
            String uns = StressUtils.unstress(word);
            if (!uns.equals(word)) {
                r = paradigmsByForm.get(word.replace("*", ""));
            }
        }
        return r;
    }
}
