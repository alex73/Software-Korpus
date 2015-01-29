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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.bind.JAXBContext;

import org.alex73.korpus.editor.core.GrammarDB;
import org.alex73.korpus.editor.core.structure.KorpusDocument;
import org.alex73.korpus.parser.TEIParser;
import org.alex73.korpus.parser.TextParser;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.io.FileUtils;

import alex73.corpus.paradigm.P;
import alex73.corpus.paradigm.Part;
import alex73.corpus.paradigm.S;
import alex73.corpus.paradigm.TEI;
import alex73.corpus.paradigm.Text;
import alex73.corpus.paradigm.W;

/**
 * Collects statistics of most frequently used lemmas.
 */
public class StatFrequency {

    public static JAXBContext CONTEXT;
    static {
        try {
            CONTEXT = JAXBContext.newInstance(TEI.class.getPackage().getName());
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

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

        List<File> files = new ArrayList<>(FileUtils.listFiles(new File("Korpus-texts/"), new String[] {
                "xml", "text", "7z", "zip" }, true));
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

        for (int i = 0; i < 50; i++) {
            System.out.print(i + 1);
            String most = null;
            long mostCount = 0;
            for (Map.Entry<String, Long> en : lemmasCount.entrySet()) {
                if (en.getValue() > mostCount) {
                    most = en.getKey();
                    mostCount = en.getValue();
                }
            }
            System.out.println(" " + most + " - " + mostCount);
            lemmasCount.remove(most);
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
            KorpusDocument doc;
            InputStream in = new BufferedInputStream(new FileInputStream(f));
            try {
                doc = TEIParser.parseXML(in);
            } finally {
                in.close();
            }
            loadTextToCorpus(doc);
        } else if (f.getName().endsWith(".text")) {
            KorpusDocument doc;
            InputStream in = new BufferedInputStream(new FileInputStream(f));
            try {
                doc = TextParser.parseText(in, false);
            } finally {
                in.close();
            }
            loadTextToCorpus(doc);
        } else {
            throw new RuntimeException("Unknown file: " + f);
        }
    }

    protected static void loadArchiveFileToCorpus(File f) throws Exception {
        if (f.getName().endsWith(".zip")) {
            ZipFile zip = new ZipFile(f);
            int c = 0;
            for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements();) {
                ZipEntry en = it.nextElement();
                if (en.isDirectory()) {
                    continue;
                }
                System.out.println("loadFileToCorpus " + f + "/" + en.getName() + ": " + (++c));
                KorpusDocument doc;
                InputStream in = new BufferedInputStream(zip.getInputStream(en));
                try {
                    if (en.getName().endsWith(".text")) {
                        doc = TextParser.parseText(in, false);
                    } else if (en.getName().endsWith(".xml")) {
                        doc = TEIParser.parseXML(in);
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
            int c = 0;
            for (SevenZArchiveEntry en = sevenZFile.getNextEntry(); en != null; en = sevenZFile
                    .getNextEntry()) {
                if (en.isDirectory()) {
                    continue;
                }
                System.out.println("loadFileToCorpus " + f + "/" + en.getName() + ": " + (++c));
                byte[] content = new byte[(int) en.getSize()];
                for (int p = 0; p < content.length;) {
                    p += sevenZFile.read(content, p, content.length - p);
                }
                try {
                    KorpusDocument doc;
                    InputStream in = new ByteArrayInputStream(content);
                    try {
                        if (en.getName().endsWith(".text")) {
                            doc = TextParser.parseText(in, false);
                        } else if (en.getName().endsWith(".xml")) {
                            doc = TEIParser.parseXML(in);
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

    protected static void loadTextToCorpus(KorpusDocument doc) throws Exception {
        Text text = TEIParser.constructXML(doc);

        List<S> sentences = new ArrayList<>();
        for (Object o : text.getBody().getHeadOrPOrDiv()) {
            if (o instanceof P) {
                P p = (P) o;
                for (Object o2 : p.getSOrTag()) {
                    if (o2 instanceof S) {
                        sentences.add((S) o2);
                    }
                }
            } else if (o instanceof Part) {
            }
        }
        stat(sentences);
    }

    protected static void stat(List<S> sentences) {
        for (S s : sentences) {
            for (Object o : s.getWOrTag()) {
                if (o instanceof W) {
                    W w = (W) o;
                    Long c = lemmasCount.get(w.getLemma());
                    if (c == null) {
                        c = 0L;
                    }
                    lemmasCount.put(w.getLemma(), c + 1);
                }
            }
        }
    }
}
