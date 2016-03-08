/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2013 Aleś Bułojčyk (alex73mail@gmail.com)
               Home page: https://sourceforge.net/projects/korpus/

 This file is part of Korpus.

 Korpus is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Korpus is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.alex73.korpus.editor.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.alex73.korpus.text.parser.BOMBufferedReader;
import org.alex73.korpus.utils.StressUtils;
import org.alex73.korpus.utils.WordNormalizer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import alex73.corpus.paradigm.Paradigm;
import alex73.corpus.paradigm.Paradigm.Form;
import alex73.corpus.paradigm.Wordlist;

/**
 * Сховішча поўнай граматычнай базы ў памяці, з магчымасьцю рэдагаваньня.
 */
public class GrammarDB {
    public static final Locale BEL = new Locale("be");

    public static final JAXBContext CONTEXT;
    public static final Schema schema;

    private static GrammarDB instance;

    List<Paradigm> allParadigms = new ArrayList<>();
    List<Paradigm> docLevelParadigms = new ArrayList<>();
    Map<String, Paradigm[]> paradigmsByForm = new HashMap<>();
    String znaki = "";
    String letters = "ёйцукенгшўзх'фывапролджэячсмітьбющиЁЙЦУКЕНГШЎЗХ'ФЫВАПРОЛДЖЭЯЧСМІТЬБЮЩИqwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";

    Map<String, Theme> themes = new TreeMap<>();

    static {
        try {
            CONTEXT = JAXBContext.newInstance(Wordlist.class.getPackage().getName());
            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            schema = factory.newSchema(GrammarDB.class.getResource("/xsd/Paradigm.xsd"));
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static synchronized GrammarDB getInstance() {
        return instance;
    }

    public static synchronized void initializeFromJar(LoaderProgress progress) throws Exception {
        if (instance != null) {
            return;
        }
        GrammarDB ins = new GrammarDB(progress);
        if (!ins.allParadigms.isEmpty()) {
            instance = ins;
        }
    }

    public static synchronized void initializeFromDir(File dir, LoaderProgress progress) throws Exception {
        if (instance != null) {
            return;
        }
        instance = new GrammarDB(dir, progress);
    }

    private GrammarDB(LoaderProgress progress) throws Exception {
        try (InputStream in = GrammarDB.class.getResourceAsStream("/themes.txt")) {
            if (in == null)
                return;
            addThemesFile(in);
        }

        progress.setFilesCount(25);
        for (char c = 'A'; c <= 'Z'; c++) {
            System.out.println(c + ".xml");
            progress.beforeFileLoading(c + ".xml");
            InputStream in = GrammarDB.class.getResourceAsStream("/" + c + ".xml");
            if (in != null) {
                addXMLFile(in, false);
                in.close();
            }
            progress.afterFileLoading();
        }

        stat();
    }

    private GrammarDB(File dir, LoaderProgress progress) throws Exception {
        long be = System.currentTimeMillis();
        addThemesFile(new File(dir, "themes.txt"));
        File[] xmlFiles = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile()
                        && (pathname.getName().endsWith(".xml") || pathname.getName().endsWith(".xml.gz"));
            }
        });
        progress.setFilesCount(xmlFiles.length);
        for (int i = 0; i < xmlFiles.length; i++) {
            System.out.println(xmlFiles[i].getPath());
            progress.beforeFileLoading(xmlFiles[i].getName());
            addXMLFile(xmlFiles[i], false);
            progress.afterFileLoading();
        }
        stat();
        long af = System.currentTimeMillis();
        System.out.println("GrammarDB loading time: " + (af - be) + "ms");

        be = System.currentTimeMillis();
        Kryo kryo = new Kryo();
        Output output = new Output(new FileOutputStream("/tmp/file.bin"), 65536);
        kryo.writeObject(output, allParadigms);
        output.close();
        af = System.currentTimeMillis();
        System.out.println("GrammarDB serialization time: " + (af - be) + "ms");
        
//        be = System.currentTimeMillis();
//        Input input = new Input(new FileInputStream("/tmp/file.bin"), 65536);
//        List<Paradigm> someObject = kryo.readObject(input, ArrayList.class);
//        input.close();
//        af = System.currentTimeMillis();
//        System.out.println("GrammarDB deserialization time: " + (af - be) + "ms");
    }

    protected GrammarDB() {
    }

    public void stat() {
        int fill = 6;
        DecimalFormat fo = new DecimalFormat("###,###,##0");
        Map<String, Integer> counts = new TreeMap<>();
        for (Paradigm p : allParadigms) {
            String k = p.getTag().substring(0, Math.min(fill, p.getTag().length()));
            Integer c = counts.get(k);
            if (c == null) {
                c = 0;
            }
            counts.put(k, c + 1);
        }
        System.out.println("=========================== " + fill + " ===========================");
        int c = 0;
        for (String k : counts.keySet()) {
            String ff = fo.format(counts.get(k).intValue());
            System.out.println(
                    k + "          ".substring(k.length()) + "           ".substring(ff.length()) + ff);
            c += counts.get(k);
        }
        System.out.println("Total: " + fo.format(c));
    }

    public synchronized void addXMLFile(InputStream file, boolean docLevel) throws Exception {

        Unmarshaller unm = CONTEXT.createUnmarshaller();

        InputStream in = new BufferedInputStream(file, 65536);
        try {
            Wordlist words = (Wordlist) unm.unmarshal(in);
            for (Paradigm p : words.getParadigm()) {
                addParadigm(p);
                if (docLevel) {
                    docLevelParadigms.add(p);
                }
            }
        } finally {
            in.close();
        }
    }

    public synchronized void addXMLFile(File file, boolean docLevel) throws Exception {
        if (file.getName().equals("clusters.xml")) {
            return;
        }
        Unmarshaller unm = CONTEXT.createUnmarshaller();

        InputStream in;
        if (file.getName().endsWith(".gz")) {
            in = new BufferedInputStream(new GZIPInputStream(new FileInputStream(file), 16384), 65536);
        } else {
            in = new BufferedInputStream(new FileInputStream(file), 65536);
        }
        try {
            Wordlist words = (Wordlist) unm.unmarshal(in);
            for (Paradigm p : words.getParadigm()) {
                addParadigm(p);
                if (docLevel) {
                    docLevelParadigms.add(p);
                }
            }
        } finally {
            in.close();
        }
    }

    public synchronized void addThemesFile(InputStream file) throws Exception {
        BOMBufferedReader rd = new BOMBufferedReader(new InputStreamReader(file, "UTF-8"));
        String s;
        String part = "";
        while ((s = rd.readLine()) != null) {
            s = s.trim();
            if (s.length() == 1) {
                part = s;
            } else if (s.length() > 1) {
                addTheme(part, s);
            }
        }
        rd.close();
    }

    public synchronized void addThemesFile(File file) throws Exception {
        BOMBufferedReader rd = new BOMBufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"));
        String s;
        String part = "";
        while ((s = rd.readLine()) != null) {
            s = s.trim();
            if (s.length() == 1) {
                part = s;
            } else if (s.length() > 1) {
                addTheme(part, s);
            }
        }
        rd.close();
    }

    public Theme getThemes(String grammar) {
        return themes.get(grammar);
    }

    public void saveDocLevelParadygms(File outFile) throws Exception {
        Marshaller m = CONTEXT.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        Wordlist list = new Wordlist();
        list.getParadigm().addAll(docLevelParadigms);
        m.marshal(list, outFile);
    }

    void addTheme(String part, String theme) {
        Theme th = themes.get(part);
        if (th == null) {
            th = new Theme("");
            themes.put(part, th);
        }
        for (String p : theme.split("/")) {
            th = th.getOrCreateChild(p);
        }
    }

    public synchronized void addDocLevelParadigm(Paradigm p) {
        addParadigm(p);
        docLevelParadigms.add(p);
    }

    private void addParadigm(Paradigm p) {
        // optimize(p);
        if (p.getTag()!=null) {
        p.setTag(p.getTag().intern());
        }
        allParadigms.add(p);
        for (Form f : p.getForm()) {
            if (f.getTag()!=null) {
            f.setTag(f.getTag().intern());
            }
            if (f.getSlouniki()!=null) {
            f.setSlouniki(f.getSlouniki().intern());
            }
            if (f.getPravapis()!=null) {
            f.setPravapis(f.getPravapis().intern());
            }
            if (f.getValue()!=null) {
                f.setValue(f.getValue().intern());
            }
            String v = WordNormalizer.normalize(f.getValue());
            if (v.isEmpty()) {
                continue;
            }
            Paradigm[] byForm = paradigmsByForm.get(v);
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
            paradigmsByForm.put(v, byForm);
        }
        if (p.getTheme() != null) {
            if (p.getTag() != null && p.getTag().length() > 0) {
                String part = p.getTag().substring(0, 1);
                for (String t : p.getTheme().split(";")) {
                    t = t.trim();
                    if (!t.isEmpty()) {
                        addTheme(part, t);
                    }
                }
            }
        }
        if (p.getTag().startsWith("K")) {
            for (Form f : p.getForm()) {
                if (f.getValue().length() != 1) {
                    throw new RuntimeException(
                            "Незразумелы знак з кодам '" + p.getTag() + "': " + f.getValue());
                }
                znaki += f.getValue();
            }
        }
    }

    public Paradigm parseAndValidate(String pText) throws Exception {
        Validator validator = schema.newValidator();

        Source source = new StreamSource(new StringReader(pText));
        validator.validate(source);

        Unmarshaller unm = GrammarDB.CONTEXT.createUnmarshaller();
        Paradigm p = (Paradigm) unm.unmarshal(new StringReader(pText));
        // check stress
        for (Paradigm.Form f : p.getForm()) {
            StressUtils.checkStress(f.getValue());
        }
        return p;
    }

    void optimize(Paradigm p) {
        p.setLemma(optimizeString(p.getLemma()));
        p.setTag(optimizeString(p.getTag()));
        for (int i = 0; i < p.getForm().size(); i++) {
            Paradigm.Form f = p.getForm().get(i);
            f.setTag(optimizeString(f.getTag()));
            f.setValue(optimizeString(f.getValue()));
        }
    }

    String optimizeString(String s) {
        return s == null ? null : s.intern();
    }

    public synchronized List<Paradigm> getAllParadigms() {
        return Collections.unmodifiableList(allParadigms);
    }

    public synchronized Paradigm getLooksLike(String word, String looksLike, boolean checkForms,
            String tagMask, StringBuilder out) {
        Paradigm ratedParadigm = null;
        String ratedForm = null;

        looksLike = looksLike.trim();
        if (looksLike.isEmpty()) {
            int rating = 1;
            String find = WordNormalizer.normalize(word);
            for (Paradigm p : allParadigms) {
                if (!isTagLooksLikeMask(p.getTag(), tagMask)) {
                    continue;
                }
                int eq = compareEnds(find, WordNormalizer.normalize(p.getLemma()));
                if (eq > rating) {
                    rating = eq;
                    ratedParadigm = p;
                    ratedForm = p.getLemma();
                }
                if (checkForms) {
                    for (Paradigm.Form f : p.getForm()) {
                        eq = compareEnds(find, WordNormalizer.normalize(f.getValue()));
                        if (eq > rating) {
                            rating = eq;
                            ratedParadigm = p;
                            ratedForm = f.getValue();
                        }
                    }
                }
            }
        } else {
            String find = WordNormalizer.normalize(looksLike);
            for (Paradigm p : allParadigms) {
                if (!isTagLooksLikeMask(p.getTag(), tagMask)) {
                    continue;
                }
                if (find.equals(WordNormalizer.normalize(p.getLemma()))) {
                    ratedParadigm = p;
                    ratedForm = p.getLemma();
                }
                if (checkForms) {
                    for (Paradigm.Form f : p.getForm()) {
                        if (find.equals(WordNormalizer.normalize(f.getValue()))) {
                            ratedParadigm = p;
                            ratedForm = f.getValue();
                        }
                    }
                }
            }
        }
        if (ratedParadigm == null) {
            return null;
        }
        out.append(ratedParadigm.getLemma() + "/" + ratedParadigm.getTag());
        return constructParadigm(word, ratedParadigm, ratedForm);
    }

    /**
     * Mask can be just start of tag, and can contains '?'.
     */
    public static boolean isTagLooksLikeMask(String tag, String mask) {
        if (mask.isEmpty()) {
            return true;
        }
        if (mask.length() > tag.length()) {
            return false;
        }
        for (int i = 0; i < mask.length(); i++) {
            char cT = tag.charAt(i);
            char cM = mask.charAt(i);
            if (cT != cM && cM != '?') {
                return false;
            }
        }
        return true;
    }

    public String getZnaki() {
        return znaki;
    }

    public String getLetters() {
        return letters;
    }

    /**
     * Find paradigms by word (unstressed, lower case).
     */
    public synchronized Paradigm[] getParadigmsByForm(String word) {
        return paradigmsByForm.get(WordNormalizer.normalize(word));
    }

    /**
     * Construct new paradigm based on specified paradigm.
     * 
     * Stress syll based on rules:
     * 
     * 1. usually stressed chars
     * 
     * 2. otherwise, the same syll from word start if 'word' has stress
     * 
     * 3. otherwise, the same syll from word end if paradigm has stress
     */
    Paradigm constructParadigm(String word, Paradigm p, String ratedForm) {
        String unstressedWord = WordNormalizer.normalize(word);
        String unstressedRatedForm = WordNormalizer.normalize(ratedForm);
        int eq = compareEnds(unstressedWord, unstressedRatedForm);
        int ratedSkip = unstressedRatedForm.length() - eq;
        Paradigm result = new Paradigm();
        result.setTag(p.getTag());

        int stressInSource = StressUtils.getStressFromStart(word);

        String lemma = constructWord(unstressedWord, eq, WordNormalizer.normalize(p.getLemma()), ratedSkip);
        int st = StressUtils.getUsuallyStressedSyll(lemma);
        if (st < 0) {
            st = stressInSource;
        }
        if (st >= 0) {
            lemma = StressUtils.setStressFromStart(lemma, st);
        } else {
            st = StressUtils.getStressFromEnd(p.getLemma());
            lemma = StressUtils.setStressFromEnd(lemma, st);
        }

        result.setLemma(lemma);

        for (Paradigm.Form f : p.getForm()) {
            Paradigm.Form rf = new Paradigm.Form();
            rf.setTag(f.getTag());
            String fword = constructWord(unstressedWord, eq, WordNormalizer.normalize(f.getValue()),
                    ratedSkip);
            if (!fword.isEmpty()) {
                st = StressUtils.getUsuallyStressedSyll(fword);
                if (st < 0) {
                    st = stressInSource;
                }
                if (st >= 0) {
                    fword = StressUtils.setStressFromStart(fword, st);
                } else {
                    st = StressUtils.getStressFromEnd(f.getValue());
                    fword = StressUtils.setStressFromEnd(fword, st);
                }
            }
            rf.setValue(fword);
            result.getForm().add(rf);
        }
        return result;
    }

    String constructWord(String originalWord, int eq, String form, int formSkip) {
        if (form.length() < formSkip) {
            return "????????????????";
        }
        String origBeg = originalWord.substring(0, originalWord.length() - eq);
        String formEnd = form.substring(formSkip);
        return origBeg + formEnd;
    }

    int compareEnds(String w1, String w2) {
        int eq = 0;
        for (int i1 = w1.length() - 1, i2 = w2.length() - 1; i1 >= 0 && i2 >= 0; i1--, i2--) {
            if (w1.charAt(i1) != w2.charAt(i2)) {
                break;
            }
            eq++;
        }
        return eq;
    }

    public interface LoaderProgress {
        void setFilesCount(int count);

        void beforeFileLoading(String file);

        void afterFileLoading();
    }
}
