package org.alex73.korpus.base;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.Poetry;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.XMLText;
import org.alex73.korpus.utils.SetUtils;
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
 * 
 * Гэты варыянт аптымізаваны для найхутчэйшай працы пры нязменнай базе. Усе
 * дакладныя варыянты тэгаў і лемаў ствараюцца на старце з базы.
 */
public class StaticGrammarFiller {
    public static final Locale BEL = new Locale("be");

    private Map<String, String> lemmas = new HashMap<>();
    private Map<String, String> tags = new HashMap<>();

    public StaticGrammarFiller(GrammarDB2 gr) {
        long be = System.currentTimeMillis();
        gr.getAllParadigms().parallelStream().forEach(p -> {
            p.getVariant().forEach(v -> {
                v.getForm().forEach(f -> {
                    if (f.getValue()==null) {
                        return;
                    }
                    String formTag = SetUtils.tag(p, v, f);
                    if (f.getValue().indexOf('_') >= 0 || formTag.indexOf('_') >= 0) {
                        throw new RuntimeException("Слова ў базе не можа быць выкарыстана: " + p.getPdgId() + v.getId()
                                + "/" + formTag + ": " + f.getValue());
                    }
                    String orig = BelarusianWordNormalizer.normalize(f.getValue());
                    if (orig.isEmpty()) {
                        return;
                    }
                    add(lemmas, orig, p.getLemma());
                    add(tags, orig, formTag);
                    String s = StressUtils.unstress(orig);
                    if (!s.equals(orig)) {
                        add(lemmas, s, p.getLemma());
                        add(tags, s, formTag);
                    }
                });
            });
        });
        long af = System.currentTimeMillis();
        System.out.println(this.getClass().getSimpleName() + " prepare time: " + (af - be) + "ms");
    }

    static Pattern SEP = Pattern.compile("_");

    private void add(Map<String, String> map, String key, String value) {
        String oldMark = '_' + value + '_';
        synchronized (map) {
            String old = map.get(key);
            if (old == null) {
                map.put(key, oldMark);
            } else {
                if (old.contains(oldMark)) {
                    return;
                }
                old += value + '_';
                map.put(key, old);
            }
        }
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
        String word = BelarusianWordNormalizer.normalize(w.getValue());
        w.setLemma(lemmas.get(word));
        w.setCat(tags.get(word));
    }
}
