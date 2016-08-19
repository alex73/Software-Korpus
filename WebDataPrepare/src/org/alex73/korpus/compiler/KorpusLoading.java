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
import org.alex73.korpus.text.TextGeneral;
import org.alex73.korpus.text.TextIO;
import org.alex73.korpus.text.parser.IProcess;
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

    LuceneDriverWrite lucene;

    StatInfo total = new StatInfo("");

    Set<String> authors = new TreeSet<>();
    IProcess errors;
    PrepareCache.CallbackP callback;

    public KorpusLoading(IProcess errors, PrepareCache.CallbackP callback) {
        this.errors = errors;
        this.callback = callback;
    }

    void processKorpus() throws Exception {
        File dir = new File("Korpus-cache/");
        FileUtils.deleteDirectory(dir);
        dir.mkdirs();
        lucene = new LuceneDriverWrite("Korpus-cache/");

        Properties stat = new Properties();

        List<File> files = new ArrayList<>(
                FileUtils.listFiles(new File("Korpus-texts/"), new String[] { "xml", "text", "7z", "zip" }, true));
        if (files.isEmpty()) {
            System.out.println("Няма тэкстаў ў Korpus-texts/");
            System.exit(1);
        }
        Collections.shuffle(files);
        int c = 0;
        for (File f : files) {
            errors.showStatus("loadFileToCorpus " + f + ": " + (++c) + "/" + files.size());
            if (f.getName().endsWith(".xml") || f.getName().endsWith(".text")) {
                loadXmlOrTextFileToCorpus(f);
            } else if (f.getName().endsWith(".zip") || f.getName().endsWith(".7z")) {
                loadArchiveFileToCorpus(f);
            }
        }
        for (StatInfo si : parts.values()) {
            // si.write(stat); - don't output details yet
        }
        total.write(stat);

        errors.showStatus("Optimize...");
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
        errors.showStatus(total.texts + " files processed");
    }

    protected void loadXmlOrTextFileToCorpus(File f) throws Exception {
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
            try {
                XMLText doc = new TextGeneral(f, errors).parse();
                loadTextToCorpus(doc);
            } catch (Exception ex) {
                throw new RuntimeException("Памылка ў " + f + ": " + ex.getMessage(), ex);
            }
        } else {
            throw new RuntimeException("Unknown file: " + f);
        }
    }

    protected void loadArchiveFileToCorpus(File f) throws Exception {
        if (f.getName().endsWith(".zip")) {
            try (ZipFile zip = new ZipFile(f)) {
                int c = 0;
                for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements();) {
                    ZipEntry en = it.nextElement();
                    if (en.isDirectory()) {
                        continue;
                    }
                    errors.showStatus("loadFileToCorpus " + f + "/" + en.getName() + ": " + (++c));
                    XMLText doc;
                    InputStream in = new BufferedInputStream(zip.getInputStream(en));
                    try {
                        if (en.getName().endsWith(".text")) {
                            doc = new TextGeneral(in, errors).parse();
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
            for (SevenZArchiveEntry en = sevenZFile.getNextEntry(); en != null; en = sevenZFile.getNextEntry()) {
                if (en.isDirectory()) {
                    continue;
                }
                errors.showStatus("loadFileToCorpus " + f + "/" + en.getName() + ": " + (++c));
                byte[] content = new byte[(int) en.getSize()];
                for (int p = 0; p < content.length;) {
                    p += sevenZFile.read(content, p, content.length - p);
                }
                try {
                    XMLText doc;
                    try (InputStream in = new ByteArrayInputStream(content)) {
                        if (en.getName().endsWith(".text")) {
                            doc = new TextGeneral(in, errors).parse();
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

    protected void loadTextToCorpus(XMLText doc) throws Exception {

        List<P> sentences = new ArrayList<>();
        for (Object o : doc.getContent().getPOrTagOrPoetry()) {
            if (o instanceof P) {
                sentences.add((P) o);
            }
        }
        sentences = Utils.randomizeOrder(sentences);

        sentences.parallelStream().forEach(p -> callback.processP(p));

        AtomicInteger wordsCount = new AtomicInteger();
        sentences.parallelStream().forEach(p -> wordsCount.addAndGet(processP(p)));

        total.addText(wordsCount.get());
    }

    int processP(P p) {
        try {
            byte[] pa = new BinaryParagraphWriter().write(p);
            int c = lucene.addSentence(p, pa);
            return c;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
