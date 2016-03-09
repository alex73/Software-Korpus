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
import java.util.Comparator;
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
    public static final String CACHE_FILE = "db.cache";
    public static final String THEMES_FILE = "themes.txt";

    public static final String letters = "ёйцукенгшўзх'фывапролджэячсмітьбющиЁЙЦУКЕНГШЎЗХ'ФЫВАПРОЛДЖЭЯЧСМІТЬБЮЩИqwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";

    public static final JAXBContext CONTEXT;
    public static final Schema schema;

    private static GrammarDB instance;

    Map<String, Theme> themes;
    List<Paradigm> allParadigms = new ArrayList<>();
    Map<String, Paradigm[]> paradigmsByForm = new HashMap<>();
    String znaki = "";

    transient List<Paradigm> docLevelParadigms = new ArrayList<>();
    transient Map<String, Paradigm[]> docLevelParadigmsByForm = new HashMap<>();

    static {
        try {
            CONTEXT = JAXBContext.newInstance(Wordlist.class.getPackage().getName());
            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            schema = factory.newSchema(GrammarDB.class.getResource("/xsd/Paradigm.xsd"));
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    protected GrammarDB() {
    }

    public static synchronized GrammarDB getInstance() {
        return instance;
    }

    public static synchronized void initializeFromJar(LoaderProgress progress) throws Exception {
        if (instance != null) {
            return;
        }
        try (InputStream in = GrammarDB.class.getResourceAsStream("/" + CACHE_FILE)) {
            if (in == null)
                return;
            long be = System.currentTimeMillis();
            Input input = new Input(in, 65536);
            instance = loadFromCache(input);
            long af = System.currentTimeMillis();
            System.out.println("GrammarDB deserialization time: " + (af - be) + "ms");
        }

        instance.stat();
    }

    public static synchronized void initializeFromDir(File dir, LoaderProgress progress) throws Exception {
        if (instance != null) {
            return;
        }

        File[] forLoads = getFilesForLoad(dir);
        File cacheFile = new File(dir, CACHE_FILE);
        if (cacheExist(dir, cacheFile, forLoads)) {
            long be = System.currentTimeMillis();
            try (Input input = new Input(new FileInputStream(cacheFile), 65536)) {
                instance = loadFromCache(input);
            }
            long af = System.currentTimeMillis();
            System.out.println("GrammarDB deserialization time: " + (af - be) + "ms");
            instance.stat();
        } else {
            instance = new GrammarDB(dir, forLoads, cacheFile, progress);
        }
    }

    private GrammarDB(File dir, File[] forLoads, File cacheFile, LoaderProgress progress) throws Exception {
        long be = System.currentTimeMillis();
        progress.setFilesCount(forLoads.length);
        long latest = 0;
        for (int i = 0; i < forLoads.length; i++) {
            latest = Math.max(latest, forLoads[i].lastModified());
            System.out.println(forLoads[i].getPath());
            progress.beforeFileLoading(forLoads[i].getName());
            if (forLoads[i].getName().equals(THEMES_FILE)) {
                addThemesFile(forLoads[i]);
            } else {
                addXMLFile(forLoads[i], false);
            }
            progress.afterFileLoading();
        }
        stat();
        long af = System.currentTimeMillis();
        System.out.println("GrammarDB loading time: " + (af - be) + "ms");

        be = System.currentTimeMillis();
        Kryo kryo = new Kryo();
        try (Output output = new Output(new FileOutputStream(cacheFile), 65536)) {
            kryo.writeObject(output, this);
        }
        cacheFile.setLastModified(latest);
        af = System.currentTimeMillis();
        System.out.println("GrammarDB serialization time: " + (af - be) + "ms");
    }

    private static GrammarDB loadFromCache(Input input) throws Exception {
        Kryo kryo = new Kryo();
        return kryo.readObject(input, GrammarDB.class);
    }

    /**
     * Get files for load(xml and themes.txt)
     */
    private static File[] getFilesForLoad(File dir) throws Exception {
        File[] result = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile() && (pathname.getName().endsWith(".xml")
                        || pathname.getName().endsWith(".xml.gz") || pathname.getName().equals(THEMES_FILE));
            }
        });
        Arrays.sort(result, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                String n1 = o1.getName();
                String n2 = o2.getName();
                if (n1.equals(THEMES_FILE)) {
                    return -1;
                } else {
                    return n1.compareTo(n2);
                }
            }
        });
        return result;
    }

    /**
     * Returns true if cache file exist and last modified equals to latest xml file.
     */
    private static boolean cacheExist(File dir, File cacheFile, File[] filesForLoad) throws Exception {
        if (!cacheFile.exists()) {
            return false;
        }
        long latest = 0;
        for (File f : filesForLoad) {
            latest = Math.max(latest, f.lastModified());
        }
        return cacheFile.lastModified() == latest;
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

    public synchronized void addXMLFile(File file, boolean docLevel) throws Exception {
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
                if (docLevel) {
                    addDocLevelParadigm(p);
                } else {
                    optimize(p);
                    addParadigm(p);
                }
            }
        } finally {
            in.close();
        }
    }

    public synchronized void addThemesFile(File file) throws Exception {
        themes = new TreeMap<>();
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

    public synchronized void saveDocLevelParadygms(File outFile) throws Exception {
        Marshaller m = CONTEXT.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        Wordlist list = new Wordlist();
        list.getParadigm().addAll(docLevelParadigms);
        m.marshal(list, outFile);
    }

    public synchronized void clearDocLevelParadygms() {
        docLevelParadigms.clear();
        docLevelParadigmsByForm.clear();
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

    private void addParadigm(Paradigm p) {
        allParadigms.add(p);
        for (Form f : p.getForm()) {
            String v = WordNormalizer.normalize(f.getValue());
            if (v.isEmpty()) {
                continue;
            }
            v = v.intern();
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

    public synchronized void addDocLevelParadigm(Paradigm p) {
        docLevelParadigms.add(p);
        for (Form f : p.getForm()) {
            String v = WordNormalizer.normalize(f.getValue());
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
            f.setSlouniki(optimizeString(f.getSlouniki()));
            f.setPravapis(optimizeString(f.getPravapis()));
        }
    }

    String optimizeString(String s) {
        return s == null ? null : s.intern();
    }

    public synchronized boolean isEmpty() {
        return allParadigms.isEmpty();
    }

    private static List<Paradigm> plus(List<Paradigm>... lists) {
        List<Paradigm> result = new ArrayList<>();
        for (List<Paradigm> o : lists) {
            result.addAll(o);
        }
        return result;
    }

    public synchronized Paradigm getLooksLike(String word, String looksLike, boolean checkForms,
            String tagMask, StringBuilder out) {
        Paradigm ratedParadigm = null;
        String ratedForm = null;

        looksLike = looksLike.trim();
        if (looksLike.isEmpty()) {
            int rating = 1;
            String find = WordNormalizer.normalize(word);
            for (Paradigm p : plus(allParadigms, docLevelParadigms)) {
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
            for (Paradigm p : plus(allParadigms, docLevelParadigms)) {
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
    private static boolean isTagLooksLikeMask(String tag, String mask) {
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
        Paradigm[] p1 = paradigmsByForm.get(WordNormalizer.normalize(word));
        Paradigm[] p2 = docLevelParadigmsByForm.get(WordNormalizer.normalize(word));
        if (p1 == null && p2 == null) {
            return null;
        }
        if (p1 == null) {
            p1 = new Paradigm[0];
        }
        if (p2 == null) {
            p2 = new Paradigm[0];
        }
        Paradigm[] result = new Paradigm[p1.length + p2.length];
        System.arraycopy(p1, 0, result, 0, p1.length);
        System.arraycopy(p2, 0, result, p1.length, p2.length);
        return result;
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
