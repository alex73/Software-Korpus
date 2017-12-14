package org.alex73.korpus.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.XMLText;
import org.alex73.korpus.utils.StressUtils;

public class GrammarFiller {
    public static final Locale BEL = new Locale("be");

    private IGrammarFinder[] fi;

    public GrammarFiller(IGrammarFinder... fi) {
        this.fi = fi;
    }

    private Paradigm[][] getParadigmsByWord(String word) {
        Paradigm[][] ps = new Paradigm[fi.length][];
        for (int i = 0; i < fi.length; i++) {
            ps[i] = fi[i].getParadigmsByForm(word);
        }
        return ps;
    }

    public W getWordInfo(String w) {
        String word = BelarusianWordNormalizer.normalize(w);
        W result = new W(w); // value must be original text
        Paradigm[][] ps = getParadigmsByWord(word);
        fillWordInfoParadigms(result, word, ps);
        return result;
    }

    public void fill(XMLText doc) {
        doc.getContent().getPOrTagOrPoetry().parallelStream().forEach(op -> {
            if (op instanceof P) {
                ((P) op).getSe().forEach(s -> {
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
        });
    }

    public void fill(W w) {
        String word = BelarusianWordNormalizer.normalize(w.getValue());
        Paradigm[][] ps = getParadigmsByWord(word);
        fillWordInfoParadigms(w, word, ps);
    }

    public void fillWordInfoPagadigm(W w, Paradigm paradygm) {
        String word = BelarusianWordNormalizer.normalize(w.getValue());

        Paradigm[][] ps = new Paradigm[1][1];
        ps[0][0] = paradygm;
        w.setLemma(null);
        w.setCat(null);
        fillWordInfoParadigms(w, word, ps);
    }

    public void fillWordInfoLemma(W w, String lemma) {
        String word = BelarusianWordNormalizer.normalize(w.getValue());

        Paradigm[][] ps = getParadigmsByWord(word);
        for (int i = 0; i < ps.length; i++) {
            Paradigm[] paradigms = ps[i];
            if (paradigms == null) {
                continue;
            }
            List<Paradigm> pt = new ArrayList<>(paradigms.length);
            for (Paradigm p : paradigms) {
                if (p.getLemma().equals(lemma)) {
                    pt.add(p);
                }
            }
            ps[i] = pt.toArray(new Paradigm[pt.size()]);
        }
        fillWordInfoParadigms(w, word, ps);
    }

    private void fillWordInfoParadigms(W w, String word, Paradigm[][] paradigms) {
        Set<String> lemmas = new TreeSet<>();
        Set<String> cats = new TreeSet<>();
        boolean hasStress = StressUtils.hasStress(word);
        for (Paradigm[] ps : paradigms) {
            if (ps == null) {
                continue;
            }
            for (Paradigm p : ps) {
                lemmas.add(p.getLemma());
                for (Variant v : p.getVariant()) {
                    for (Form f : v.getForm()) {
                        if (hasStress) {
                            if (word.equals(f.getValue())) {
                                cats.add(p.getTag() + f.getTag());
                            }
                        } else {
                            if (word.equals(StressUtils.unstress(f.getValue()))) {
                                cats.add(p.getTag() + f.getTag());
                            }
                        }
                    }
                }
            }
        }
        w.setLemma(set2string(lemmas));
        w.setCat(set2string(cats));
    }

    protected String set2string(Set<String> set) {
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
