package org.alex73.korpus.editor.grammar;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
TODO
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
    public Paradigm[] getParadigms(String w) {
        return null;
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

    /**
     * 
     */
    public void fill(W w) {
        staticFiller.fill(w);
        String word = BelarusianWordNormalizer.normalize(w.getValue());
        newParadigms.stream().forEach(p -> {
            p.getVariant().forEach(v -> {
                v.getForm().forEach(f -> {
                    if (f.getValue() == null) {
                        return;
                    }
                    String formTag = SetUtils.tag(p, v, f);
                    String orig = BelarusianWordNormalizer.normalize(f.getValue());
                    if (orig.isEmpty()) {
                        return;
                    }
                    if (!word.equals(orig)) {
                        return;
                    }
                    w.setLemma(addIfNeed(w.getLemma(), p.getLemma()));
                    w.setCat(addIfNeed(w.getCat(), formTag));
                });
            });
        });
    }

    private String addIfNeed(String prevList, String newPart) {
        String mark = '_' + newPart + '_';
        if (prevList.contains(mark)) {
            return prevList;
        } else if (prevList.isEmpty()) {
            return mark;
        } else {
            return prevList + newPart + '_';
        }
    }

    /**
     * User selected specific paradigm on UI.
     */
    public void fillFromPagadigm(W w, Paradigm paradygm) {
        Paradigm[][] ps = new Paradigm[1][1];
        ps[0][0] = paradygm;
        w.setLemma(null);
        w.setCat(null);
        fillWordInfoParadigms(w, ps);
    }

    private void fillWordInfoParadigms(W w, Paradigm[][] paradigms) {
        String word = BelarusianWordNormalizer.normalize(w.getValue());
        Set<String> lemmas = new TreeSet<>();
        Set<String> cats = new TreeSet<>();
        boolean hasStress = StressUtils.hasStress(word);
        for (Paradigm[] ps : paradigms) {
            if (ps == null) {
                continue;
            }
            for (Paradigm p : ps) {
                for (Variant v : p.getVariant()) {
                    for (Form f : v.getForm()) {
                        String form = BelarusianWordNormalizer.normalize(f.getValue());
                        if (hasStress) {
                            if (word.equals(form)) {
                                lemmas.add(p.getLemma());
                                cats.add(SetUtils.tag(p, v, f));
                            }
                        } else {
                            if (word.equals(StressUtils.unstress(form))) {
                                lemmas.add(p.getLemma());
                                cats.add(SetUtils.tag(p, v, f));
                            }
                        }
                    }
                }
            }
        }
        w.setLemma(set2string(lemmas));
        w.setCat(set2string(cats));
    }

    private String set2string(Set<String> set) {
        if (set.isEmpty()) {
            return null;
        }
        StringBuilder r = new StringBuilder();
        for (String s : set) {
            if (r.length() > 0) {
                r.append('_');
            }
            r.append(s);
        }
        return r.toString();
    }
}
