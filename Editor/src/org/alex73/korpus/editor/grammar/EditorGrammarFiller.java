package org.alex73.korpus.editor.grammar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.base.BelarusianWordNormalizer;
import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.StaticGrammarFiller;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.Poetry;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.XMLText;
import org.alex73.korpus.utils.SetUtils;
import org.alex73.korpus.utils.StressUtils;

public class EditorGrammarFiller {
    protected final GrammarDB2 db;
    private final StaticGrammarFiller staticFiller;
    private final List<Paradigm> newParadigms;

    public EditorGrammarFiller(GrammarDB2 db, StaticGrammarFiller staticFiller, List<Paradigm> newParadigms) {
        this.db = db;
        this.staticFiller = staticFiller;
        this.newParadigms = newParadigms;
    }

    /**
     * Returns list of paradigms that can be used for specific word.
     */
    public List<Paradigm> getParadigms(String w) {
        List<Paradigm> result=Collections.synchronizedList(new ArrayList<>());
        
        String word = BelarusianWordNormalizer.normalizePreserveCase(w);
        String wordLower = BelarusianWordNormalizer.normalizeLowerCase(w);
        boolean wordStressed = StressUtils.hasStress(word);

        db.getAllParadigms().parallelStream().forEach(p -> {
            if (checkParadigm(word, wordLower, wordStressed, p)) {
                result.add(p);
            }
        });
        newParadigms.parallelStream().forEach(p -> {
            if (checkParadigm(word, wordLower, wordStressed, p)) {
                result.add(p);
            }
        });
        return result;
    }

    public void fill(XMLText doc) {
        Consumer<P> processP = new Consumer<P>() {
            @Override
            public void accept(P op) {
                op.getSe().forEach(s -> {
                    s.getWOrSOrZ().forEach(ow -> {
                        if (ow instanceof W) {
                            W w = (W) ow;
                            if (!w.isManual()) {
                                fill(w);
                            }
                        }
                    });
                });
            }
        };
        doc.getContent().getPOrTagOrPoetry().parallelStream().forEach(op -> {
            if (op instanceof P) {
                processP.accept((P) op);
            } else if (op instanceof Poetry) {
                ((Poetry) op).getPOrTag().forEach(op2 -> {
                    if (op2 instanceof P) {
                        processP.accept((P) op2);
                    }
                });
            }
        });
    }

    public void fill(W w) {
        w.setLemma(null);
        w.setCat(null);
        staticFiller.fill(w);
        String word = BelarusianWordNormalizer.normalizePreserveCase(w.getValue());
        String wordLower = BelarusianWordNormalizer.normalizeLowerCase(w.getValue());
        boolean wordStressed = StressUtils.hasStress(word);
        newParadigms.parallelStream().forEach(p -> fillFromParadigm(w, word, wordLower, wordStressed, p));
    }


    /**
     * User selected specific paradigm on UI.
     */
    public void fillFromPagadigm(W w, Paradigm paradygm) {
        staticFiller.fill(w);
        w.setLemma(null);
        w.setCat(null);
        String word = BelarusianWordNormalizer.normalizePreserveCase(w.getValue());
        String wordLower = BelarusianWordNormalizer.normalizeLowerCase(w.getValue());
        boolean wordStressed = StressUtils.hasStress(word);
        fillFromParadigm(w, word, wordLower, wordStressed, paradygm);
    }

    private boolean checkParadigm(String word, String wordLower, boolean wordStressed, Paradigm p) {
        for(Variant v:p.getVariant()) {
            for(Form f:v.getForm()) {
                if (f.getValue() == null || f.getValue().isEmpty()) {
                    continue;
                }
                if (wordStressed) {
                    if (word.equals(f.getValue()) || wordLower.equals(f.getValue())) {
                        return true;
                    }
                } else {
                    String fUnstressed = StressUtils.unstress(f.getValue());
                    if (word.equals(fUnstressed) || wordLower.equals(fUnstressed)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void fillFromParadigm(W w, String word, String wordLower, boolean wordStressed, Paradigm p) {
        p.getVariant().forEach(v -> {
            v.getForm().forEach(f -> {
                if (f.getValue() == null || f.getValue().isEmpty()) {
                    return;
                }
                if (wordStressed) {
                    if (word.equals(f.getValue()) || wordLower.equals(f.getValue())) {
                        synchronized (w) {
                            w.setLemma(addIfNeed(w.getLemma(), p.getLemma()));
                            w.setCat(addIfNeed(w.getCat(), SetUtils.tag(p, v, f)));
                        }
                    }
                } else {
                    String fUnstressed = StressUtils.unstress(f.getValue());
                    if (word.equals(fUnstressed) || wordLower.equals(fUnstressed)) {
                        synchronized (w) {
                            w.setLemma(addIfNeed(w.getLemma(), p.getLemma()));
                            w.setCat(addIfNeed(w.getCat(), SetUtils.tag(p, v, f)));
                        }
                    }
                }
            });
        });
    }

    private String addIfNeed(String prevList, String newPart) {
        String mark = '_' + newPart + '_';
        if (prevList == null || prevList.isEmpty()) {
            return mark;
        } else if (prevList.contains(mark)) {
            return prevList;
        } else {
            return prevList + newPart + '_';
        }
    }
}
