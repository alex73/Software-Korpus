package org.alex73.korpus.base;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.corpus.paradigm.Wordlist;
import org.alex73.korpus.editor.core.Theme;
import org.alex73.korpus.text.parser.BOMBufferedReader;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class GrammarDB2 {
    public static final String CACHE_FILE = "db.cache";
    public static final String THEMES_FILE = "themes.txt";

    private Map<String, Theme> themes;
    private List<Paradigm> allParadigms = new ArrayList<>();

    public List<Paradigm> getAllParadigms() {
        return allParadigms;
    }

    public static GrammarDB2 initializeFromJar() throws Exception {
        GrammarDB2 r;
        try (InputStream in = GrammarDB2.class.getResourceAsStream("/" + CACHE_FILE)) {
            if (in == null)
                return null;
            long be = System.currentTimeMillis();
            Input input = new Input(in, 65536);
            r = loadFromCache(input);
            long af = System.currentTimeMillis();
            System.out.println("GrammarDB deserialization time: " + (af - be) + "ms");
        }
        return r;
    }

    public static GrammarDB2 initializeFromDir(String dir) throws Exception {
        GrammarDB2 r;
        File[] forLoads = getFilesForLoad(new File(dir));
        File cacheFile = new File(dir, CACHE_FILE);
        if (cacheExist(new File(dir), cacheFile, forLoads)) {
            long be = System.currentTimeMillis();
            try (Input input = new Input(new FileInputStream(cacheFile), 65536)) {
                r = loadFromCache(input);
            }
            long af = System.currentTimeMillis();
            System.out.println("GrammarDB deserialization time: " + (af - be) + "ms");
        } else {
            r = new GrammarDB2(new File(dir), forLoads, cacheFile);
        }
        return r;
    }

    /**
     * Get files for load(xml and themes.txt)
     */
    public static File[] getFilesForLoad(File dir) throws Exception {
        File[] result = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile() && (pathname.getName().endsWith(".xml")
                        || pathname.getName().endsWith(".xml.gz") || pathname.getName().equals(THEMES_FILE));
            }
        });
        if (result == null) {
            throw new Exception("There are no files for GrammarDB in the " + dir.getAbsolutePath());
        }
        /*
         * Arrays.sort(result, new Comparator<File>() {
         * 
         * @Override public int compare(File o1, File o2) { String n1 =
         * o1.getName(); String n2 = o2.getName(); if (n1.equals(THEMES_FILE)) {
         * return -1; } else if (n2.equals(THEMES_FILE)) { return 1; } else {
         * return n1.compareTo(n2); } } });
         */
        return result;
    }

    public static void removeCache(File dir) throws Exception {
        File cacheFile = new File(dir, CACHE_FILE);
        if (cacheFile.exists()) {
            if (!cacheFile.delete()) {
                throw new Exception("Can't remove cache file");
            }
        }
    }

    /**
     * Returns true if cache file exist and last modified equals to latest xml
     * file.
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

    private static GrammarDB2 loadFromCache(Input input) throws Exception {
        Kryo kryo = new Kryo();
        return kryo.readObject(input, GrammarDB2.class);
    }

    private void addTheme(String part, String theme) {
        Theme th = themes.get(part);
        if (th == null) {
            th = new Theme("");
            themes.put(part, th);
        }
        for (String p : theme.split("/")) {
            th = th.getOrCreateChild(p);
        }
    }

    public void addThemesFile(File file) throws Exception {
        themes = new TreeMap<>();
        BOMBufferedReader rd = new BOMBufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
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

    /**
     * Minimize memory usage.
     */
    private void optimize(Paradigm p) {
        p.setLemma(optimizeString(p.getLemma()));
        p.setTag(optimizeString(p.getTag()));
        for (Variant v : p.getVariant()) {
            for (Form f : v.getForm()) {
                f.setTag(optimizeString(f.getTag()));
                f.setValue(optimizeString(f.getValue()));
                f.setSlouniki(optimizeString(f.getSlouniki()));
                f.setPravapis(optimizeString(f.getPravapis()));
            }
        }
    }

    /**
     * Remove duplicate strings from memory.
     */
    private String optimizeString(String s) {
        return s == null ? null : s.intern();
    }

    private void addXMLFile(File file, JAXBContext ctx) throws Exception {
        Unmarshaller unm = ctx.createUnmarshaller();

        InputStream in;
        if (file.getName().endsWith(".gz")) {
            in = new BufferedInputStream(new GZIPInputStream(new FileInputStream(file), 16384), 65536);
        } else {
            in = new BufferedInputStream(new FileInputStream(file), 65536);
        }
        try {
            Wordlist words = (Wordlist) unm.unmarshal(in);
            for (Paradigm p : words.getParadigm()) {
                optimize(p);
            }
            allParadigms.addAll(words.getParadigm());
        } finally {
            in.close();
        }
    }

    /**
     * Only for kryo instantiation.
     */
    private GrammarDB2() {
    }

    /**
     * Read xml files for initialize.
     */
    private GrammarDB2(File dir, File[] forLoads, File cacheFile) throws Exception {
        long be = System.currentTimeMillis();

        JAXBContext ctx = JAXBContext.newInstance(Wordlist.class.getPackage().getName());

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(16);
        long latest = 0;
        for (int i = 0; i < forLoads.length; i++) {
            latest = Math.max(latest, forLoads[i].lastModified());
            System.out.println(forLoads[i].getPath());
            final File process = forLoads[i];
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (process.getName().equals(THEMES_FILE)) {
                            addThemesFile(process);
                        } else {
                            addXMLFile(process, ctx);
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
        }
        executor.shutdown();
        if (!executor.awaitTermination(2, TimeUnit.MINUTES)) {
            throw new Exception("Load GrammarDB2 timeout");
        }
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

    public Theme getThemes(String grammar) {
        return themes.get(grammar);
    }
}
