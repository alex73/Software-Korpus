/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2013-2015 Aleś Bułojčyk (alex73mail@gmail.com)
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamResult;

import org.alex73.korpus.editor.core.GrammarDB;
import org.alex73.korpus.editor.core.structure.KorpusDocument;
import org.alex73.korpus.parser.TEIParser;
import org.alex73.korpus.parser.TextParser;
import org.alex73.korpus.server.engine.LuceneDriverKorpus;
import org.alex73.korpus.server.engine.LuceneDriverOther;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.io.FileUtils;

import alex73.corpus.paradigm.P;
import alex73.corpus.paradigm.Part;
import alex73.corpus.paradigm.S;
import alex73.corpus.paradigm.TEI;
import alex73.corpus.paradigm.Text;

/**
 * Class for loading Corpus texts into searchable cache.
 */
public class KorpusLoading {

    static Random RANDOM = new Random();

    static LuceneDriverKorpus lucene;

    static int statTexts, statWords;
    static int textId;
    static Set<String> authors = new TreeSet<>();

    static void processKorpus() throws Exception {
        File dir =new File("Korpus-cache/"); 
        FileUtils.deleteDirectory(dir);
        dir.mkdirs();
        lucene = new LuceneDriverKorpus("Korpus-cache/", true);

        Properties stat = new Properties();
        int allStatTexts = 0, allStatWords = 0;
        statTexts = 0;
        statWords = 0;
        List<File> files = new ArrayList<>(FileUtils.listFiles(new File("Korpus-texts/"), new String[] {
                "xml", "text", "7z", "zip" }, true));
        if (files.isEmpty()) {
            System.out.println("Няма тэкстаў ў Korpus-texts/");
            System.exit(1);
        }
        Collections.sort(files);
        int c = 0;
        for (File f : files) {
            System.out.println("loadFileToCorpus " + f + ": " + (++c) + "/" + files.size());
            statTexts = 0;
            statWords = 0;
            if (f.getName().endsWith(".xml") || f.getName().endsWith(".text")) {
                loadXmlOrTextFileToCorpus(f);
            } else if (f.getName().endsWith(".zip") || f.getName().endsWith(".7z")) {
                loadArchiveFileToCorpus(f);
                stat.setProperty("texts." + f.getName(), "" + statTexts);
                stat.setProperty("words." + f.getName(), "" + statWords);
            }
            allStatTexts += statTexts;
            allStatWords += statWords;
        }
        stat.setProperty("texts", "" + allStatTexts);
        stat.setProperty("words", "" + allStatWords);

        System.out.println("Optimize...");
        lucene.shutdown();

        String authorsstr = "";
        for (String s : authors) {
            authorsstr += ";" + s;
        }

        if (!authors.isEmpty()) {
            stat.setProperty("authors", authorsstr.substring(1));
        } else {
            stat.setProperty("authors", "");
        }
        FileOutputStream o = new FileOutputStream("Korpus-cache/stat.properties");
        stat.store(o, null);
        o.close();
        System.out.println(statTexts + " files processed");
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
            try (ZipFile zip = new ZipFile(f)) {
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
            }
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
                    try (InputStream in = new ByteArrayInputStream(content)) {
                        if (en.getName().endsWith(".text")) {
                            doc = TextParser.parseText(in, false);
                        } else if (en.getName().endsWith(".xml")) {
                            doc = TEIParser.parseXML(in);
                        } else {
                            throw new RuntimeException("Unknown entry '" + en.getName() + "' in " + f);
                        }
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
        statTexts++;
        textId++;

        for (String a : doc.textInfo.authors) {
            authors.add(a);
        }
        Text text = TEIParser.constructXML(doc);

        Marshaller m = TEIParser.CONTEXT.createMarshaller();
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        StreamResult mOut = new StreamResult(ba);
        int id = 1;
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
        sentences = randomizeOrder(sentences);
        for (S s : sentences) {
            ba.reset();
            m.marshal(s, mOut);
            String stylegenre = "";
            String[] authors = new String[0];
            lucene.addSentence(s, ba.toByteArray(), textId, doc.textInfo);
            statWords += s.getWOrTag().size();
            id++;
        }

        String title = "";
        for (String a : doc.textInfo.authors) {
            title += ", " + a;
        }
        if (!title.isEmpty()) {
            title = title.substring(2) + ". " + doc.textInfo.title;
        } else {
            title = doc.textInfo.title;
        }
        lucene.addText(textId, doc.textInfo);
    }

    static List<S> randomizeOrder(List<S> sentences) {
        List<S> result = new ArrayList<>(sentences.size());
        while (!sentences.isEmpty()) {
            int next = RANDOM.nextInt(sentences.size());
            result.add(sentences.remove(next));
        }
        return result;
    }
}
