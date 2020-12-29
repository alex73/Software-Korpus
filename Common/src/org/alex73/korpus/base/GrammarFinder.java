package org.alex73.korpus.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.alex73.corpus.paradigm.Paradigm;

public class GrammarFinder implements IGrammarFinder {
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
                putToPrepare(v.getLemma(), prepare, p);
                v.getForm().forEach(f -> {
                    if (f.getValue() != null && !f.getValue().isEmpty()) {
                        putToPrepare(f.getValue(), prepare, p);
                    }
                });
            });
        });
        table = prepareToFinal(prepare);
        long af = System.currentTimeMillis();
        System.out.println("GrammarFinder prepare time: " + (af - be) + "ms");
    }

    private void putToPrepare(String w, List<List<Paradigm>> prepare, Paradigm p) {
        int hash = BelarusianWordNormalizer.hash(w);
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

    private Paradigm[][] prepareToFinal(List<List<Paradigm>> prepare) {
        Paradigm[][] result = new Paradigm[prepare.size()][];
        int maxLen = 0;
        for (int i = 0; i < result.length; i++) {
            List<Paradigm> list = prepare.get(i);
            if (!list.isEmpty()) {
                result[i] = list.toArray(new Paradigm[list.size()]);
                maxLen = Math.max(maxLen, result[i].length);
            }
        }
        System.out.println("GrammarFinder max table tail: " + maxLen);
        return result;
    }

    /**
     * Find paradigms by lemma or form (lower case).
     */
    public Paradigm[] getParadigms(String word) {
        int hash = BelarusianWordNormalizer.hash(word);
        int indexByHash = Math.abs(hash) % HASHTABLE_SIZE;
        Paradigm[] result = table[indexByHash];
        return result != null ? result : EMPTY;
    }

    public Stream<Paradigm[]> getSimilarGroups() {
        return Arrays.stream(table).filter(r -> r != null);
    }
}
