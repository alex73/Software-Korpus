package org.alex73.korpus.base;

import java.util.Arrays;
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

/**
 * Як шукаем парадыгмы для слова.
 * 
 * Шукаем без уліку вялікіх/малых літараў. Няма сэнсу нешта адмысловае рабіць
 * для ўласных імёнаў, бо спатыкнемся на назвах накшталт "Вялікае Княства
 * Літоўскае" - словы з вялікай літары будуць разглядацца як уласныя імёны, што
 * няправільна.
 * 
 * Націск мусіць быць "несупярэчлівым". То бок калі ў тэксце слова з націскам -
 * выбіраем парадыгмы з базы з такім самым націскамі ці без націску, але не з
 * іншым націскам.
 */
public class GrammarFiller {
    public static final Locale BEL = new Locale("be");

    private IGrammarFinder[] fi;

    public GrammarFiller(IGrammarFinder... fi) {
        this.fi = fi;
    }

    public Paradigm[][] getParadigmsByWord(String word) {
        String normalizedWord = BelarusianWordNormalizer.normalize(word);
        Paradigm[][] ps = new Paradigm[fi.length][];
        for (int i = 0; i < fi.length; i++) {
            Paradigm[] list = fi[i].getParadigmsLikeForm(word);
            Paradigm[] outList = new Paradigm[list.length];
            int p2 = 0;
            for (int p1 = 0; p1 < list.length; p1++) {
                if (isAllowedForWord(normalizedWord, list[p1])) {
                    outList[p2] = list[p1];
                    p2++;
                }
            }
            ps[i] = Arrays.copyOf(outList, p2);
        }
        return ps;
    }

    boolean isAllowedForWord(String normalizedWord, Paradigm p) {
        for (Variant v : p.getVariant()) {
            for (Form f : v.getForm()) {
                String normalizedForm = BelarusianWordNormalizer.normalize(f.getValue());
                if (normalizedForm.equals(normalizedWord)) {
                    return true;
                }
            }
        }
        return false;
    }

    public W getWordInfo(String w) {
        String word = BelarusianWordNormalizer.normalize(w);
        W result = new W(w); // value must be original text
        Paradigm[][] ps = getParadigmsByWord(word);
        fillWordInfoParadigms(result, ps);
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
        fillWordInfoParadigms(w, ps);
    }

    public void fillWordInfoPagadigm(W w, Paradigm paradygm) {
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
                                cats.add(p.getTag() + f.getTag());
                            }
                        } else {
                            if (word.equals(StressUtils.unstress(form))) {
                                lemmas.add(p.getLemma());
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
