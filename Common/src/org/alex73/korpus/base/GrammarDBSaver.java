package org.alex73.korpus.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.FormOptions;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.corpus.paradigm.Wordlist;
import org.alex73.korpus.utils.StressUtils;

public class GrammarDBSaver {

    private static String getFileForParadigm(Paradigm p) {
        if (p.getTag().startsWith("NP")) {
            return "NP.xml";
        } else if (p.getTag().length() > 1 && p.getTag().charAt(1) == '+') {
            return p.getTag().substring(0, 1) + "_.xml";
        } else {
            return p.getTag().substring(0, 1) + ".xml";
        }
    }

    public static void store(File out, Wordlist list) throws Exception {
        Marshaller m = GrammarDB2.getContext().createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(list, out);
    }

    public static void store(List<Paradigm> ps, File out) throws Exception {
        Marshaller m = GrammarDB2.getContext().createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        Wordlist w = new Wordlist();
        w.getParadigm().addAll(ps);
        m.marshal(w, out);
    }

    public static void sortAndStore(GrammarDB2 db, String dir) throws Exception {
        sortAndStore(db.getAllParadigms(), new File(dir));
    }

    public static void sortAndStore(List<Paradigm> ps, File dir) throws Exception {
        Map<String, Wordlist> es = new TreeMap<>();
        for (Paradigm p : ps) {
            String fn = getFileForParadigm(p);
            Wordlist w = es.get(fn);
            if (w == null) {
                w = new Wordlist();
                es.put(fn, w);
            }
            w.getParadigm().add(p);
        }
        dir.mkdirs();
        Marshaller m = GrammarDB2.getContext().createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        for (String fn : es.keySet()) {
            Wordlist wl=es.get(fn);
            Collections.sort(wl.getParadigm(), COMPARATOR);
            m.marshal(wl, new File(dir, fn));
        }
    }

    public static void sortList(List<Paradigm> ps) {
        Collections.sort(ps, COMPARATOR);
    }

    public static void store(OutputStream out, Wordlist list) throws Exception {
        Marshaller m = GrammarDB2.getContext().createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(list, out);
    }

    public static Paradigm cloneParadigm(Paradigm p) throws Exception {
        Marshaller m = GrammarDB2.getContext().createMarshaller();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.marshal(p, out);
        Unmarshaller unm = GrammarDB2.getContext().createUnmarshaller();
        Paradigm r = (Paradigm) unm.unmarshal(new ByteArrayInputStream(out.toByteArray()));
        return r;
    }
    public static Variant cloneVariant(Variant v) throws Exception {
        Marshaller m = GrammarDB2.getContext().createMarshaller();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.marshal(v, out);
        Unmarshaller unm = GrammarDB2.getContext().createUnmarshaller();
        Variant r = (Variant) unm.unmarshal(new ByteArrayInputStream(out.toByteArray()));
        return r;
    }

    /**
     * TODO: сартаваць склоны файл A: сартуем: па 1 літары(род) : MNFP 2
     * (склон): NGDAIL 3 (лік): SP файл N: (ад прыметнікаў - як прыметнікі)
     * формы MNS - мужчынскі сартуем: па 1 літары(склон): NGDAIL 2 (лік): SP
     */
    static Comparator<Form> COMPARATOR_FORM_PRYM = new Comparator<Form>() {
        public int compare(Form o1, Form o2) {
            try {
                if (o1.getTag().isEmpty() || o2.getTag().isEmpty()) {
                    return o1.getTag().length() - o2.getTag().length();
                }
                int p1 = "MNFPX".indexOf(o1.getTag().charAt(0));
                int p2 = "MNFPX".indexOf(o2.getTag().charAt(0));
                if (p1 == p2) {
                    p1 = "SPX".indexOf(o1.getTag().charAt(2));
                    p2 = "SPX".indexOf(o2.getTag().charAt(2));
                }
                if (p1 == p2) {
                    p1 = "NGDAIL".indexOf(o1.getTag().charAt(1));
                    p2 = "NGDAIL".indexOf(o2.getTag().charAt(1));
                }
                if (p1 == p2) {
                    p1=formOptionIndex(o1.getOptions());
                    p2=formOptionIndex(o2.getOptions());
                }
                return p1 - p2;
            } catch (StringIndexOutOfBoundsException ex) {
                return 0;
            }
        }

        int formOptionIndex(FormOptions opt) {
            if (opt == null) {
                return 0;
            } else if (opt == FormOptions.ANIM) {
                return 1;
            } else {
                return 2;
            }
        }
    };

    static Comparator<Form> COMPARATOR_FORM_NAZ = new Comparator<Form>() {
        public int compare(Form o1, Form o2) {
            try {
                if (o1.getTag().isEmpty() || o2.getTag().isEmpty()) {
                    return o1.getTag().length() - o2.getTag().length();
                }
                int p1 = "SP".indexOf(o1.getTag().charAt(1));
                int p2 = "SP".indexOf(o2.getTag().charAt(1));
                if (p1 == p2) {
                    p1 = "NGDAIL".indexOf(o1.getTag().charAt(0));
                    p2 = "NGDAIL".indexOf(o2.getTag().charAt(0));
                }
                return p1 - p2;
            } catch (StringIndexOutOfBoundsException ex) {
                return 0;
            }
        }
    };

    public static Locale BE = new Locale("be");
    public static Collator BEL = Collator.getInstance(BE);

    public static Comparator<Paradigm> COMPARATOR = new Comparator<Paradigm>() {
        @Override
        public int compare(Paradigm p1, Paradigm p2) {
            if (p1.getLemma().equals("абго+ртвальны")&&p2.getLemma().equals("абго+ртвальны")) {
                System.out.println();
            }
            String w1 = StressUtils.unstress(p1.getLemma().toLowerCase(BE));
            String w2 = StressUtils.unstress(p2.getLemma().toLowerCase(BE));
            int r = BEL.compare(w1.toLowerCase(), w2.toLowerCase());
            if (r == 0) {
                r = BEL.compare(p1.getLemma().toLowerCase(), p2.getLemma().toLowerCase());
            }
            if (r == 0) {
                r = BEL.compare(p1.getLemma(), p2.getLemma());
            }
            if (r == 0) {
                String m1 = p1.getMeaning();
                String m2 = p2.getMeaning();
                if (m1 == null)
                    m1 = "";
                if (m2 == null)
                    m2 = "";
                r = BEL.compare(m1, m2);
            }
            if (r == 0) {
                r = Integer.compare(p1.getPdgId(), p2.getPdgId());
            }
            return r;
        }
    };
}
