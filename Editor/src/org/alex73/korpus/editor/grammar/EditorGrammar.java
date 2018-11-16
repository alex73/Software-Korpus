package org.alex73.korpus.editor.grammar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.corpus.paradigm.Wordlist;
import org.alex73.korpus.base.BelarusianWordNormalizer;
import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.IGrammarFinder;
import org.alex73.korpus.editor.core.Theme;
import org.alex73.korpus.utils.StressUtils;

public class EditorGrammar implements IGrammarFinder {

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

    public GrammarDB2 gr;
    private String localGrammarFile;

    private List<Paradigm> docLevelParadigms = new ArrayList<>();
    private Map<String, Paradigm[]> docLevelParadigmsByForm = new HashMap<>();

    public EditorGrammar(GrammarDB2 gr, String localGrammarFile) throws Exception {
        this.gr = gr;
        this.localGrammarFile = localGrammarFile;

        File f = new File(localGrammarFile);
        if (f.exists()) {
            try (InputStream in = new BufferedInputStream(new FileInputStream(localGrammarFile), 65536)) {
                Wordlist words = (Wordlist) CONTEXT.createUnmarshaller().unmarshal(in);
                for (Paradigm p : words.getParadigm()) {
                    addDocLevelParadigm(p);
                }
            }
        }
    }

    public synchronized List<Paradigm> getAllParadigms() {
        List<Paradigm> r = new ArrayList<>(gr.getAllParadigms());
        r.addAll(docLevelParadigms);
        return r;
    }

    @Override
    public synchronized Paradigm[] getParadigmsByForm(String word) {
        word = StressUtils.unstress(BelarusianWordNormalizer.normalize(word));
        Paradigm[] r = docLevelParadigmsByForm.get(word);
        return r;
    }

    public synchronized void addDocLevelParadigm(Paradigm p) {
        docLevelParadigms.add(p);
        for (Variant va : p.getVariant()) {
            for (Form f : va.getForm()) {
                String v = StressUtils.unstress(BelarusianWordNormalizer.normalize(f.getValue()));
                if (v.isEmpty()) {
                    continue;
                }
                Paradigm[] byForm = docLevelParadigmsByForm.get(v);
                if (byForm == null) {
                    byForm = new Paradigm[1];
                } else {
                    if (byForm[byForm.length - 1] == p) {
                        // already stored
                        continue;
                    }
                    byForm = Arrays.copyOf(byForm, byForm.length + 1);
                }
                byForm[byForm.length - 1] = p;
                docLevelParadigmsByForm.put(v, byForm);
            }
        }
    }

    /*
     * public void fillWordInfoParadigms(W w, String word, Paradigm[] paradigms)
     * { Set<String> lemmas = new TreeSet<>(); Set<String> cats = new
     * TreeSet<>(); if (paradigms != null) { for (Paradigm p : paradigms) {
     * lemmas.add(p.getLemma()); boolean foundForm = false; for(Variant
     * v:p.getVariant()) { for (Form f : v.getForm()) { if
     * (word.equals(f.getValue())) { cats.add(p.getTag() + f.getTag());
     * foundForm = true; } }} if (!foundForm) { // the same find, but without
     * stress and lowercase String uw =
     * BelarusianWordNormalizer.normalize(word); for(Variant v:p.getVariant()) {
     * for (Form f : v.getForm()) { if
     * (uw.equals(BelarusianWordNormalizer.normalize(f.getValue()))) {
     * cats.add(p.getTag() + f.getTag()); } }} } } }
     * w.setLemma(SetUtils.set2string(lemmas));
     * w.setCat(SetUtils.set2string(cats)); }
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
        return gr.getThemes(grammar);
    }

    public synchronized void save() throws Exception {
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
        list.getParadigm().addAll(docLevelParadigms);
        m.marshal(list, out);
    }

}
