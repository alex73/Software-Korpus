package org.alex73.korpus.base;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.korpus.utils.StressUtils;

public class GrammarFinder implements IGrammarFinder {
    private final Map<String, Paradigm[]> paradigmsByForm;

    public GrammarFinder(GrammarDB2 gr) {
        paradigmsByForm = new HashMap<>(gr.getAllParadigms().size());
        long be = System.currentTimeMillis();
        gr.getAllParadigms().parallelStream().forEach(p -> {
            p.getVariant().forEach(v -> {
                v.getForm().forEach(f -> {
                    String orig = StressUtils.unstress(BelarusianWordNormalizer.normalize(f.getValue()));
                    if (!orig.isEmpty()) {
                        add(orig, p);
                    }
                });
            });
        });
        long af = System.currentTimeMillis();
        System.out.println("GrammarFinder prepare time: " + (af - be) + "ms, forms indexed: " + paradigmsByForm.size());
    }

    /**
     * Must be synchronized because executed in the many thread by constructor.
     */
    private synchronized void add(String key, Paradigm p) {
        key = GrammarDB2.optimizeString(key);
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
        word = StressUtils.unstress(BelarusianWordNormalizer.normalize(word));
        return paradigmsByForm.get(word);
    }

    public int size() {
        return paradigmsByForm.size();
    }
}
