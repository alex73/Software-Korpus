package org.alex73.korpus.base;

import java.util.ArrayList;
import java.util.List;

import org.alex73.corpus.paradigm.Paradigm;

public class GrammarFinder implements IGrammarFinder {
    private static final int HASHTABLE_SIZE_FORMS = 512 * 1024;
    private static final int HASHTABLE_SIZE_LEMMAS = 128 * 1024;
    private static final Paradigm[] EMPTY = new Paradigm[0];
    private final Paradigm[][] tableByFormValue, tableByVariantLemma;

    public GrammarFinder(GrammarDB2 gr) {
        final List<List<Paradigm>> prepareByFormValue = new ArrayList<>(HASHTABLE_SIZE_FORMS);
        final List<List<Paradigm>> prepareByVariantLemma = new ArrayList<>(HASHTABLE_SIZE_LEMMAS);
        for (int i = 0; i < HASHTABLE_SIZE_FORMS; i++) {
            prepareByFormValue.add(new ArrayList<>());
        }
        for (int i = 0; i < HASHTABLE_SIZE_LEMMAS; i++) {
            prepareByVariantLemma.add(new ArrayList<>());
        }
        long be = System.currentTimeMillis();
        gr.getAllParadigms().parallelStream().forEach(p -> {
            p.getVariant().forEach(v -> {
                putToPrepare(v.getLemma(), prepareByVariantLemma, p);
                v.getForm().forEach(f -> {
                    if (f.getValue() != null && !f.getValue().isEmpty()) {
                        putToPrepare(f.getValue(), prepareByFormValue, p);
                    }
                });
            });
        });
        tableByFormValue = prepareToFinal(prepareByFormValue, "by form");
        tableByVariantLemma = prepareToFinal(prepareByVariantLemma, "by lemma");
        long af = System.currentTimeMillis();
        System.out.println("GrammarFinder prepare time: " + (af - be) + "ms");
    }

    private void putToPrepare(String w, List<List<Paradigm>> prepare, Paradigm p) {
        int hash = BelarusianWordHash.hash(w);
        int indexByHash = Math.abs(hash) % prepare.size();
        List<Paradigm> list = prepare.get(indexByHash);
        synchronized (list) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) == p) {
                    return;
                }
            }
            list.add(p);
        }
    }

    private Paradigm[][] prepareToFinal(List<List<Paradigm>> prepare, String desc) {
        Paradigm[][] result = new Paradigm[prepare.size()][];
        int maxLen = 0;
        for (int i = 0; i < result.length; i++) {
            List<Paradigm> list = prepare.get(i);
            if (!list.isEmpty()) {
                result[i] = list.toArray(new Paradigm[list.size()]);
                maxLen = Math.max(maxLen, result[i].length);
            }
        }
        System.out.println("GrammarFinder max table tail(" + desc + ")=" + maxLen);
        return result;
    }

    /**
     * Find paradigms by lemma (lower case).
     */
    public Paradigm[] getParadigmsLikeLemma(String word) {
        int hash = BelarusianWordHash.hash(word);
        int indexByHash = Math.abs(hash) % tableByVariantLemma.length;
        Paradigm[] result = tableByVariantLemma[indexByHash];
        return result != null ? result : EMPTY;
    }

    /**
     * Find paradigms by word (lower case).
     */
    public Paradigm[] getParadigmsLikeForm(String word) {
        int hash = BelarusianWordHash.hash(word);
        int indexByHash = Math.abs(hash) % tableByFormValue.length;
        Paradigm[] result = tableByFormValue[indexByHash];
        return result != null ? result : EMPTY;
    }
}
