package org.alex73.korpus.editor.grammar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.corpus.paradigm.Wordlist;
import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarDBSaver;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.editor.core.Theme;

public class EditorGrammar {
    static final String HEADER_PREFIX = "=====";
    static final Pattern RE_HEADER_PARADIGM = Pattern.compile("=+");
    static final Pattern RE_HEADER_VARIANT = Pattern.compile("=+\\s+([0-9]+)");
    static final Pattern RE_HEADER_FORMS = Pattern.compile("=+\\s+([0-9]+)([a-z])");

    static final JAXBContext CONTEXT;
    static final Schema schema;
    static {
        try {
            CONTEXT = JAXBContext.newInstance(Wordlist.class.getPackage().getName());
            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            schema = factory.newSchema(EditorGrammar.class.getResource("/xsd/Paradigm.xsd"));
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public EditorGrammarFiller filler;
    private String localGrammarFile;

    private GrammarDB2 db;
    // цалкам створаныя парадыгмы
    private List<Paradigm> docLevelParadigms = new ArrayList<>();
    // спіс усіх зменаў
    private List<Custom> customs = new ArrayList<>();

    public EditorGrammar(GrammarDB2 db, StaticGrammarFiller2 staticFiller, String localGrammarFile) throws Exception {
        filler = new EditorGrammarFiller(db, staticFiller, docLevelParadigms);
        this.localGrammarFile = localGrammarFile;

        Path f = Paths.get(localGrammarFile);
        if (Files.exists(f)) {
            List<String> buffer = new ArrayList<>();
            Custom c = new Custom();
            for (String s : Files.readAllLines(f)) {
                if (s.startsWith(HEADER_PREFIX)) {
                    if (!buffer.isEmpty()) {
                        c.load(buffer);
                        c.apply();
                    }
                    Matcher m;
                    if ((m = RE_HEADER_PARADIGM.matcher(s)).matches()) {
                    } else if ((m = RE_HEADER_VARIANT.matcher(s)).matches()) {
                        c.pdgId = Integer.parseInt(m.group(1));
                    } else if ((m = RE_HEADER_FORMS.matcher(s)).matches()) {
                        c.pdgId = Integer.parseInt(m.group(1));
                        c.variant = m.group(2);
                    }
                    buffer.clear();
                    c = new Custom();
                } else {
                    buffer.add(s);
                }
            }
            if (!buffer.isEmpty()) {
                c.load(buffer);
                c.apply();
            }
        }
    }

    public List<Paradigm> getAllParadigms() {
        List<Paradigm> r = new ArrayList<>(filler.db.getAllParadigms());
        synchronized (docLevelParadigms) {
            r.addAll(docLevelParadigms);
        }
        return r;
    }

    @Deprecated
    public void addDocLevelParadigm(Paradigm p) {
    }

    public void addParadigm(Paradigm p) throws Exception {
        try (BufferedWriter wr = Files.newBufferedWriter(Paths.get(localGrammarFile), StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
            wr.write("=====");
            wr.write("\n");
            Marshaller m = CONTEXT.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.setProperty(Marshaller.JAXB_FRAGMENT, true);
            m.marshal(p, wr);
            wr.write("\n");
        }
        Custom c = new Custom();
        c.p = p;
        c.apply();
    }

    public void addVariant(Variant v, int pdgId) throws Exception {
        try (BufferedWriter wr = Files.newBufferedWriter(Paths.get(localGrammarFile), StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
            wr.write("===== " + pdgId);
            wr.write("\n");
            Marshaller m = CONTEXT.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.setProperty(Marshaller.JAXB_FRAGMENT, true);
            m.marshal(v, wr);
            wr.write("\n");
        }
        Custom c = new Custom();
        c.pdgId = pdgId;
        c.v = v;
        c.apply();
    }

    public void addForms(List<Form> forms, int pdgId, String variantId) throws Exception {
        try (BufferedWriter wr = Files.newBufferedWriter(Paths.get(localGrammarFile), StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
            wr.write("===== " + pdgId + variantId);
            wr.write("\n");
            Marshaller m = CONTEXT.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.setProperty(Marshaller.JAXB_FRAGMENT, true);
            for (Form f : forms) {
                m.marshal(f, wr);
                wr.write("\n");
            }
        }
        Custom c = new Custom();
        c.pdgId = pdgId;
        c.variant = variantId;
        c.forms = forms;
        c.apply();
    }

    /*
     * public void fillWordInfoParadigms(W w, String word, Paradigm[] paradigms) {
     * Set<String> lemmas = new TreeSet<>(); Set<String> cats = new TreeSet<>(); if
     * (paradigms != null) { for (Paradigm p : paradigms) {
     * lemmas.add(p.getLemma()); boolean foundForm = false; for(Variant
     * v:p.getVariant()) { for (Form f : v.getForm()) { if
     * (word.equals(f.getValue())) { cats.add(p.getTag() + f.getTag()); foundForm =
     * true; } }} if (!foundForm) { // the same find, but without stress and
     * lowercase String uw = BelarusianWordNormalizer.normalize(word); for(Variant
     * v:p.getVariant()) { for (Form f : v.getForm()) { if
     * (uw.equals(BelarusianWordNormalizer.normalize(f.getValue()))) {
     * cats.add(p.getTag() + f.getTag()); } }} } } }
     * w.setLemma(SetUtils.set2string(lemmas)); w.setCat(SetUtils.set2string(cats));
     * }
     */

    /**
     * Find paradigms by word (lower case).
     */
    /*
     * public Paradigm[] getParadigmsByForm(String word) { word =
     * BelarusianWordNormalizer.normalize(word); Paradigm[] r =
     * paradigmsByForm.get(word); if (r == null) { String uns =
     * StressUtils.unstress(word); if (!uns.equals(word)) { r =
     * paradigmsByForm.get(word.replace("*", "")); } } return r; }
     * 
     * public W getWordInfo(String w) {
     * 
     * }
     * 
     * public void fillWordInfoLemma(W w, String lemma) {
     * 
     * }
     */

    public Theme getThemes(String grammar) {
        return filler.db.getThemes(grammar);
    }

    public void save() throws Exception {
        File out = new File(localGrammarFile);
        if (out.exists()) {
            File bak = new File(localGrammarFile + ".bak");
            bak.delete();
            if (!out.renameTo(bak)) {
                throw new Exception("Error saving local grammar file");
            }
        }

        Marshaller m = CONTEXT.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        Wordlist list = new Wordlist();
        synchronized (docLevelParadigms) {
            list.getParadigm().addAll(docLevelParadigms);
        }
        m.marshal(list, out);
    }

    class Custom {
        int pdgId = -1;
        String variant = null;
        Paradigm p;
        Variant v;
        List<Form> forms;

        void load(List<String> data) throws Exception {
            Unmarshaller unm = CONTEXT.createUnmarshaller();
            if (variant != null) { // add some forms
                forms = data.stream().map(s -> {
                    try {
                        return (Form) unm.unmarshal(new StringReader(s));
                    } catch (JAXBException ex) {
                        throw new RuntimeException(ex);
                    }
                }).collect(Collectors.toList());
            } else if (pdgId != -1) { // add some variant
                v = (Variant) unm.unmarshal(new StringReader(String.join(" ", data)));
            } else { // add full paradigm
                p = (Paradigm) unm.unmarshal(new StringReader(String.join(" ", data)));
            }
        }

        void apply() throws Exception {
            customs.add(this);

            Paradigm r;
            if (forms != null) { // add some forms
                r = getPrevParadigm();
                r = GrammarDBSaver.cloneParadigm(r);
                getVariant(r).getForm().addAll(forms);
            } else if (v != null) {
                r = getPrevParadigm();
                r = GrammarDBSaver.cloneParadigm(r);
                r.getVariant().add(v);
            } else {
                r = p;
            }
            synchronized (docLevelParadigms) {
                docLevelParadigms.add(r);
            }
        }

        Paradigm getPrevParadigm() {
            List<Paradigm> prev = db.getAllParadigms().parallelStream().filter(p -> p.getPdgId() == pdgId).toList();
            if (prev.size() != 1) {
                throw new RuntimeException("Impossible to build upon #" + pdgId + ": " + prev.size() + " previous counts");
            }
            return prev.get(0);
        }

        Variant getVariant(Paradigm prev) {
            List<Variant> r = prev.getVariant().stream().filter(v -> variant.equals(v.getId())).toList();
            if (r.size() != 1) {
                throw new RuntimeException("Wrong variant id");
            }
            return r.get(0);
        }
    }
}
