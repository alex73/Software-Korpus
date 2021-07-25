package org.alex73.korpus.base;

import java.util.HashMap;
import java.util.Map;

public class GrammarMorphFinder {
    private final Map<String, String> morphs = new HashMap<>();

    public GrammarMorphFinder(GrammarDB2 gr) {
        gr.getAllParadigms().forEach(p -> {
            p.getVariant().forEach(v -> {
                v.getMorph().forEach(m -> {
                    String key = m.replace("-", "");
                    morphs.put(key, m);
                });
            });
        });
    }

    public String get(String w) {
        return morphs.get(w);
    }
}
