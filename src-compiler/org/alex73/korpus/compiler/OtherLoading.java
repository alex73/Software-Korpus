/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2015 Aleś Bułojčyk (alex73mail@gmail.com)
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.korpus.parser.Splitter2;
import org.alex73.korpus.server.engine.LuceneDriverWrite;
import org.alex73.korpus.server.text.BinaryParagraphWriter;
import org.alex73.korpus.text.xml.P;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Class for loading Other texts into searchable cache.
 */
public class OtherLoading {

    static LuceneDriverWrite lucene;

    static StatInfo total = new StatInfo("");
    static Set<String> volumes = new TreeSet<>();

    static Pattern RE_ID=Pattern.compile("([0-9]+).+?OCR\\-texts\\.zip");

    static void processOther() throws Exception {
        File dir = new File("Other-cache/");
        FileUtils.deleteDirectory(dir);
        dir.mkdirs();
        lucene = new LuceneDriverWrite("Other-cache/");

        Properties stat = new Properties();
        List<File> files = new ArrayList<>(FileUtils.listFiles(new File("Other-texts/"),
                new String[] { "zip" }, true));
        if (files.isEmpty()) {
            System.out.println("Няма тэкстаў ў Other-texts/");
            System.exit(1);
        }
        Collections.sort(files);
        int c = 0;
        for (File f : files) {
            PrepareCache.errors.showStatus("loadFileToOther " + f + ": " + (++c) + "/" + files.size());
            loadZipPagesToOther(f);
        }
        total.write(stat);

        PrepareCache.errors.showStatus("Optimize...");
        lucene.shutdown();

        String volumesstr = "";
        for (String s : volumes) {
            volumesstr += ";" + s;
        }

        if (!volumesstr.isEmpty()) {
            stat.setProperty("volumes", volumesstr.substring(1));
        } else {
            stat.setProperty("volumes", "");
        }
        FileOutputStream o = new FileOutputStream("Other-cache/stat.properties");
        stat.store(o, null);
        o.close();
        PrepareCache.errors.showStatus(total.texts + " files processed");
    }

    protected static void loadZipPagesToOther(File f) throws Exception {
        if (f.length()==0) {
            return;
        }

        Matcher m = RE_ID.matcher(f.getName());
        if (!m.matches()) {
            throw new Exception("Wrong name: " + f);
        }

        volumes.add("kamunikat.org");
        lucene.setOtherInfo("kamunikat.org", "http://kamunikat.org/halounaja.html?pubid=" + m.group(1));

        System.out.println("loadFileToOther " + f);

        AtomicInteger wordsCount = new AtomicInteger();
        List<String> book = new ArrayList<>();
        try (ZipFile zip = new ZipFile(f)) {
            for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements();) {
                ZipEntry en = it.nextElement();
                if (en.isDirectory() || en.getSize() == 0) {
                    continue;
                }
                String text;
                try (InputStream in = new BufferedInputStream(zip.getInputStream(en))) {
                    text = IOUtils.toString(in, "UTF-8");
                }
                book.add(text);
            }
        }
        book.parallelStream().forEach(text -> {
            try {
                int w = loadTextToCorpus(text);
                wordsCount.addAndGet(w);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        total.addText(wordsCount.get());
    }

    protected static int loadTextToCorpus(String data) throws Exception {
        P p = new Splitter2(data, false, PrepareCache.errors).getP();
        int wordsCount = 0;
        byte[] pa = new BinaryParagraphWriter().write(p);
        int c = lucene.addSentence(p, pa);
        wordsCount += c;
        return wordsCount;
    }
}
