package org.alex73.korpus.base;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.XMLText;
import org.alex73.korpus.utils.StressUtils;

public class GrammarFiller2 {
    public static final Locale BEL = new Locale("be");

    private Map<String, String> lemmas = new HashMap<>();
    private Map<String, String> tags = new HashMap<>();

    public GrammarFiller2(GrammarDB2 gr) {
        long be = System.currentTimeMillis();
        gr.getAllParadigms().parallelStream().forEach(p -> {
            p.getVariant().forEach(v -> {
                v.getForm().forEach(f -> {
                    String formTag = p.getTag() + f.getTag();
                    String orig = BelarusianWordNormalizer.normalize(f.getValue());
                    synchronized (lemmas) {
                        add(lemmas, orig, p.getLemma());
                    }
                    synchronized (tags) {
                        add(tags, orig, formTag);
                    }
                    String s = StressUtils.unstress(orig);
                    if (!s.equals(orig)) {
                        synchronized (lemmas) {
                            add(lemmas, s, p.getLemma());
                        }
                        synchronized (tags) {
                            add(tags, s, formTag);
                        }
                    }
                });
            });
        });
        long af = System.currentTimeMillis();
        System.out.println("GrammarFiller2 prepare time: " + (af - be) + "ms");
    }

    static Pattern SEP = Pattern.compile("_");

    private void add(Map<String, String> map, String key, String value) {
        String old = map.get(key);
        if (old == null) {
            map.put(key, value);
        } else {
            String[] oldvs = SEP.split(old);
            for (String o : oldvs) {
                if (o.equals(value)) {
                    return;
                }
            }
            old += '_' + value;
            map.put(key, old);
        }
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

    private void fill(W w) {
        String word = BelarusianWordNormalizer.normalize(w.getValue());
        w.setLemma(lemmas.get(word));
        w.setCat(tags.get(word));
    }
}
