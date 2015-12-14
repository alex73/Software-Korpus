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

package org.alex73.korpus.compiler;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.korpus.editor.core.GrammarDB;
import org.alex73.korpus.text.TextIO;
import org.alex73.korpus.text.parser.TextParser;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.Se;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.XMLText;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.io.FileUtils;

/**
 * Collects statistics of most frequently used lemmas.
 */
public class StatFrequency {

    static final Map<String, Long> lemmasCount = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Locale.setDefault(new Locale("be"));

        System.out.println("Load GrammarDB...");
        GrammarDB.initializeFromDir(new File("GrammarDB"), new GrammarDB.LoaderProgress() {
            public void setFilesCount(int count) {
            }

            public void beforeFileLoading(String file) {
                System.out.println("Load " + file);
            }

            public void afterFileLoading() {
            }
        });

        List<File> files = new ArrayList<>(FileUtils.listFiles(new File("Korpus-texts/"),
                new String[] { "xml", "text", "7z", "zip" }, true));
        if (files.isEmpty()) {
            System.out.println("Няма тэкстаў ў Korpus-texts/");
            System.exit(1);
        }
        Collections.sort(files);
        int c = 0;
        for (File f : files) {
            System.out.println("parse " + f + ": " + (++c) + "/" + files.size());
            if (f.getName().endsWith(".xml") || f.getName().endsWith(".text")) {
                loadXmlOrTextFileToCorpus(f);
            } else if (f.getName().endsWith(".zip") || f.getName().endsWith(".7z")) {
                loadArchiveFileToCorpus(f);
            }
        }

        List<Pair> lemmas = new ArrayList<>();
        for (Map.Entry<String, Long> en : lemmasCount.entrySet()) {
            Pair p = new Pair();
            p.lemma = en.getKey();
            p.count = en.getValue();
            lemmas.add(p);
        }

        Collections.sort(lemmas, new Comparator<Pair>() {
            @Override
            public int compare(Pair o1, Pair o2) {
                if (o1.count > o2.count) {
                    return -1;
                } else if (o1.count < o2.count) {
                    return 1;
                } else {
                    return o1.lemma.compareToIgnoreCase(o2.lemma);
                }
            }
        });
        try (BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream("/tmp/freq.txt"), "UTF-8"))) {
            for (Pair p : lemmas) {
                out.write(p.lemma + "\t" + p.count + "\n");
            }
        }
    }

    static void loadGrammarDb() throws Exception {
        GrammarDB.getInstance().addThemesFile(new File("GrammarDB/themes.txt"));
        File[] xmlFiles = new File("GrammarDB").listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".xml");
            }
        });
        for (int i = 0; i < xmlFiles.length; i++) {
            System.out.println("  " + xmlFiles[i]);
            GrammarDB.getInstance().addXMLFile(xmlFiles[i], false);
        }
        GrammarDB.getInstance().stat();
    }

    static void findFiles(File dir, List<File> files) {
        File[] fs = dir.listFiles();
        if (fs == null) {
            return;
        }
        for (File f : fs) {
            if (f.isDirectory()) {
                findFiles(f, files);
            } else if (f.getName().endsWith(".xml")) {
                files.add(f);
            } else if (f.getName().endsWith(".text")) {
                files.add(f);
            } else if (f.getName().endsWith(".zip")) {
                files.add(f);
            }
        }
    }

    protected static void loadXmlOrTextFileToCorpus(File f) throws Exception {
        if (f.getName().endsWith(".xml")) {
            XMLText doc;
            InputStream in = new BufferedInputStream(new FileInputStream(f));
            try {
                doc = TextIO.parseXML(in);
            } finally {
                in.close();
            }
            loadTextToCorpus(doc);
        } else if (f.getName().endsWith(".text")) {
            XMLText doc;
            InputStream in = new BufferedInputStream(new FileInputStream(f));
            try {
                doc = TextParser.parseText(in, false, PrepareCache.errors);
            } finally {
                in.close();
            }
            loadTextToCorpus(doc);
        } else {
            throw new RuntimeException("Unknown file: " + f);
        }
    }

    protected static void loadArchiveFileToCorpus(File f) throws Exception {
        System.out.println("loadFileToCorpus " + f);
        if (f.getName().endsWith(".zip")) {
            ZipFile zip = new ZipFile(f);
            for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements();) {
                ZipEntry en = it.nextElement();
                if (en.isDirectory()) {
                    continue;
                }
                XMLText doc;
                InputStream in = new BufferedInputStream(zip.getInputStream(en));
                try {
                    if (en.getName().endsWith(".text")) {
                        doc = TextParser.parseText(in, false, PrepareCache.errors);
                    } else if (en.getName().endsWith(".xml")) {
                        doc = TextIO.parseXML(in);
                    } else {
                        throw new RuntimeException("Unknown entry '" + en.getName() + "' in " + f);
                    }
                } finally {
                    in.close();
                }
                loadTextToCorpus(doc);
            }
            zip.close();
        } else if (f.getName().endsWith(".7z")) {
            SevenZFile sevenZFile = new SevenZFile(f);
            for (SevenZArchiveEntry en = sevenZFile.getNextEntry(); en != null; en = sevenZFile
                    .getNextEntry()) {
                if (en.isDirectory()) {
                    continue;
                }
                byte[] content = new byte[(int) en.getSize()];
                for (int p = 0; p < content.length;) {
                    p += sevenZFile.read(content, p, content.length - p);
                }
                try {
                    XMLText doc;
                    InputStream in = new ByteArrayInputStream(content);
                    try {
                        if (en.getName().endsWith(".text")) {
                            doc = TextParser.parseText(in, false, PrepareCache.errors);
                        } else if (en.getName().endsWith(".xml")) {
                            doc = TextIO.parseXML(in);
                        } else {
                            throw new RuntimeException("Unknown entry '" + en.getName() + "' in " + f);
                        }
                    } finally {
                        in.close();
                    }
                    loadTextToCorpus(doc);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            sevenZFile.close();
        } else {
            throw new RuntimeException("Unknown file: " + f);
        }
    }

    protected static void loadTextToCorpus(XMLText text) throws Exception {
        List<Se> sentences = new ArrayList<>();
        for (Object o : text.getContent().getPOrTagOrPoetry()) {
            if (o instanceof P) {
                P p = (P) o;
                for (Se se : p.getSe()) {
                    sentences.add(se);
                }
            }
        }
        stat(sentences);
    }

    protected static void stat(List<Se> sentences) {
        for (Se se : sentences) {
            for (Object o : se.getWOrSOrZ()) {
                if (o instanceof W) {
                    W w = (W) o;
                    if (w.getLemma() == null) {
                        continue;
                    }
                    String[] ls = w.getLemma().split("_");
                    for (String l : ls) {
                        Long c = lemmasCount.get(l);
                        if (c == null) {
                            c = 0L;
                        }
                        lemmasCount.put(l, c + 1);
                    }
                }
            }
        }
    }

    static class Pair {
        String lemma;
        long count;
    }
}
