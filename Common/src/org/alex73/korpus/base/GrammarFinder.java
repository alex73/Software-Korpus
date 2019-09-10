package org.alex73.korpus.base;

import java.util.ArrayList;
import java.util.List;

import org.alex73.corpus.paradigm.Paradigm;

public class GrammarFinder {
    private static final int HASHTABLE_SIZE = 256 * 1024;
    private static final Paradigm[] EMPTY = new Paradigm[0];
    private final Paradigm[][] table;

    public GrammarFinder(GrammarDB2 gr) {
        final List<List<Paradigm>> prepare = new ArrayList<>(HASHTABLE_SIZE);
        for (int i = 0; i < HASHTABLE_SIZE; i++) {
            prepare.add(new ArrayList<>());
        }
        long be = System.currentTimeMillis();
        gr.getAllParadigms().parallelStream().forEach(p -> {
            p.getVariant().forEach(v -> {
                v.getForm().forEach(f -> {
                    if (f.getValue() != null && !f.getValue().isEmpty()) {
                        int hash = BelarusianWordHash.hash(f.getValue());
                        int indexByHash = Math.abs(hash) % HASHTABLE_SIZE;
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
                });
            });
        });
        table = new Paradigm[HASHTABLE_SIZE][];
        int maxLen = 0;
        for (int i = 0; i < table.length; i++) {
            List<Paradigm> list = prepare.get(i);
            if (!list.isEmpty()) {
                table[i] = list.toArray(new Paradigm[list.size()]);
                maxLen = Math.max(maxLen, table[i].length);
            }
        }
        long af = System.currentTimeMillis();
        System.out.println("GrammarFinder prepare time: " + (af - be) + "ms, with max table tail=" + maxLen);
    }

    /**
     * Find paradigms by word (lower case).
     */
    public Paradigm[] getParadigmsLikeForm(String word) {
        int hash = BelarusianWordHash.hash(word);
        int indexByHash = Math.abs(hash) % HASHTABLE_SIZE;
        Paradigm[] result = table[indexByHash];
        return result != null ? result : EMPTY;
    }
}
