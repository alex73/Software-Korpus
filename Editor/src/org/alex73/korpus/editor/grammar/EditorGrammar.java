package org.alex73.korpus.editor.grammar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Wordlist;
import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.editor.core.Theme;

public class EditorGrammar {

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

    private List<Paradigm> docLevelParadigms = new ArrayList<>();

    public EditorGrammar(GrammarDB2 db, StaticGrammarFiller2 staticFiller, String localGrammarFile) throws Exception {
        filler = new EditorGrammarFiller(db, staticFiller, docLevelParadigms);
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

    public List<Paradigm> getAllParadigms() {
        List<Paradigm> r = new ArrayList<>(filler.db.getAllParadigms());
        synchronized (docLevelParadigms) {
            r.addAll(docLevelParadigms);
        }
        return r;
    }

    public  void addDocLevelParadigm(Paradigm p) {
        synchronized (docLevelParadigms) {
            docLevelParadigms.add(p);
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
}
