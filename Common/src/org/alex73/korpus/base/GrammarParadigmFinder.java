package org.alex73.korpus.base;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;

public class GrammarParadigmFinder {
    private Map<String, Paradigm[]> paradigmsByForm = new HashMap<>();

    public GrammarParadigmFinder(GrammarDB2 gr) {
        String s;
        for (Paradigm p : gr.getAllParadigms()) {
            for (Variant v : p.getVariant()) {
                for (Form f : v.getForm()) {
                    String orig = f.getValue();
                    add(orig, p);
                    s = orig.replace("*", "");
                    if (!s.equals(orig)) {
                        add(s, p);
                    }
                }
            }
        }
    }

    private void add(String key, Paradigm p) {
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
