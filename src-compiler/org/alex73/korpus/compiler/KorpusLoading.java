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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.server.engine.LuceneDriverWrite;
import org.alex73.korpus.server.text.BinaryParagraphWriter;
import org.alex73.korpus.text.TextIO;
import org.alex73.korpus.text.parser.TextParser;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.Tag;
import org.alex73.korpus.text.xml.XMLText;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.io.FileUtils;

/**
 * Class for loading Corpus texts into searchable cache.
 */
public class KorpusLoading {

    static LuceneDriverWrite lucene;

    static StatInfo total = new StatInfo("");
    static Map<String, StatInfo> parts = new HashMap<>();

    static Set<String> authors = new TreeSet<>();

    static void processKorpus() throws Exception {
        File dir = new File("Korpus-cache/");
        FileUtils.deleteDirectory(dir);
        dir.mkdirs();
        lucene = new LuceneDriverWrite("Korpus-cache/");

        Properties stat = new Properties();

        List<File> files = new ArrayList<>(FileUtils.listFiles(new File("Korpus-texts/"), new String[] {
                "xml", "text", "7z", "zip" }, true));
        if (files.isEmpty()) {
            System.out.println("Няма тэкстаў ў Korpus-texts/");
            System.exit(1);
        }
        Collections.sort(files);
        int c = 0;
        for (File f : files) {
            PrepareCache.errors.showStatus("loadFileToCorpus " + f + ": " + (++c) + "/" + files.size());
            if (f.getName().endsWith(".xml") || f.getName().endsWith(".text")) {
                loadXmlOrTextFileToCorpus(f);
            } else if (f.getName().endsWith(".zip") || f.getName().endsWith(".7z")) {
                loadArchiveFileToCorpus(f);
            }
        }
        for (StatInfo si : parts.values()) {
            si.write(stat);
        }
        total.write(stat);

        PrepareCache.errors.showStatus("Optimize...");
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
        PrepareCache.errors.showStatus(total.texts + " files processed");
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
        if (f.getName().endsWith(".zip")) {
            try (ZipFile zip = new ZipFile(f)) {
                int c = 0;
                for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements();) {
                    ZipEntry en = it.nextElement();
                    if (en.isDirectory()) {
                        continue;
                    }
                    PrepareCache.errors.showStatus("loadFileToCorpus " + f + "/" + en.getName() + ": " + (++c));
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
            }
        } else if (f.getName().endsWith(".7z")) {
            SevenZFile sevenZFile = new SevenZFile(f);
            int c = 0;
            for (SevenZArchiveEntry en = sevenZFile.getNextEntry(); en != null; en = sevenZFile
                    .getNextEntry()) {
                if (en.isDirectory()) {
                    continue;
                }
                PrepareCache.errors.showStatus("loadFileToCorpus " + f + "/" + en.getName() + ": " + (++c));
                byte[] content = new byte[(int) en.getSize()];
                for (int p = 0; p < content.length;) {
                    p += sevenZFile.read(content, p, content.length - p);
                }
                try {
                    XMLText doc;
                    try (InputStream in = new ByteArrayInputStream(content)) {
                        if (en.getName().endsWith(".text")) {
                            doc = TextParser.parseText(in, false, PrepareCache.errors);
                        } else if (en.getName().endsWith(".xml")) {
                            doc = TextIO.parseXML(in);
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

    protected static void loadTextToCorpus(XMLText doc) throws Exception {
        TextInfo textInfo = createTextInfo(doc);
        lucene.setTextInfo(textInfo);

        for (String a : textInfo.authors) {
            authors.add(a);
        }

        List<P> sentences = new ArrayList<>();
        for (Object o : doc.getContent().getPOrTagOrPoetry()) {
            if (o instanceof P) {
                sentences.add((P) o);
            }
        }
        sentences = Utils.randomizeOrder(sentences);

        AtomicInteger wordsCount = new AtomicInteger();
        sentences.parallelStream().forEach(p -> wordsCount.addAndGet(processP(p)));

        if (textInfo.styleGenres.length > 0) {
            for (String s : textInfo.styleGenres) {
                addStat(s, wordsCount.get());
            }
        } else {
            addStat("_", wordsCount.get());
        }
        total.addText(wordsCount.get());
    }
    
    static int processP(P p) {
        try {
            byte[] pa = new BinaryParagraphWriter().write(p);
            int c = lucene.addSentence(p, pa);
            return c;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    static void addStat(String styleGenre, int wordsCount) {
        int p = styleGenre.indexOf('/');
        String st = p < 0 ? styleGenre : styleGenre.substring(0, p);
        StatInfo i = parts.get(st);
        if (i == null) {
            i = new StatInfo(st);
            parts.put(st, i);
        }
        i.addText(wordsCount);
    }

    static TextInfo createTextInfo(XMLText text) {
        TextInfo r = new TextInfo();
        String authors = getTag(text, "Authors");
        if (authors != null) {
            r.authors = authors.split(",");
        } else {
            r.authors = new String[0];
        }
        String publishedYear = getTag(text, "PublishedYear");
        r.publishedYear = publishedYear != null ? Integer.parseInt(publishedYear) : 0;
        String writtenYear = getTag(text, "WrittenYear");
        r.writtenYear = writtenYear != null ? Integer.parseInt(writtenYear) : 0;
        String styleGenres = getTag(text, "StyleGenre");
        if (styleGenres != null) {
            r.styleGenres = styleGenres.split(",");
        } else {
            r.styleGenres = new String[0];
        }
        r.title = getTag(text, "Title");

        return r;
    }

    static String getTag(XMLText text, String name) {
        for (Tag tag : text.getHeader().getTag()) {
            if (name.equals(tag.getName())) {
                return tag.getValue();
            }
        }
        return null;
    }
}
