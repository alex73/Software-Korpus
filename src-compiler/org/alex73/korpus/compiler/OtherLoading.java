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
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.korpus.editor.core.structure.Line;
import org.alex73.korpus.parser.Splitter;
import org.alex73.korpus.parser.TextParser;
import org.alex73.korpus.server.engine.LuceneDriverWrite;
import org.alex73.korpus.server.text.BinaryParagraphWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import alex73.corpus.text.P;
import alex73.corpus.text.XMLText;


/**
 * Class for loading Other texts into searchable cache.
 */
public class OtherLoading {

    static LuceneDriverWrite lucene;

    static StatInfo total = new StatInfo("");
    static Set<String> volumes = new TreeSet<>();

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
            System.out.println("loadFileToOther " + f + ": " + (++c) + "/" + files.size());
            loadZipPagesToOther(f);
        }
        total.write(stat);

        System.out.println("Optimize...");
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
        System.out.println(total.texts + " files processed");
    }

    protected static void loadZipPagesToOther(File f) throws Exception {
        int wordsCount=0;
        try (ZipFile zip = new ZipFile(f)) {
            int c = 0;
            for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements();) {
                ZipEntry en = it.nextElement();
                if (en.isDirectory()) {
                    continue;
                }
                System.out.println("loadFileToOther " + f + "/" + en.getName() + ": " + (++c));
                String text;
                try (InputStream in = new BufferedInputStream(zip.getInputStream(en))) {
                    text = IOUtils.toString(in, "UTF-8");
                }
                wordsCount+=loadTextToCorpus("kamunikat.org", "http://" + f.getName(), text);
            }
        }
        total.addText(wordsCount);
    }

    protected static int loadTextToCorpus(String volume, String textUrl, String data) throws Exception {
        volumes.add(volume);

        lucene.setOtherInfo(volume, textUrl);

        data = data.trim().replace('\n', ' ').replace('\r', ' ').replaceAll("\\s{2,}", " ");
        Line line = new Splitter(data).splitParagraph();
        line.normalize();

      XMLText text=  TextParser.constructXML(Arrays.asList(line));
        
        List<P> sentences = new ArrayList<>();
        for (Object o : text.getContent().getPOrTag()) {
            if (o instanceof P) {
                sentences.add((P) o);
            }
        }
        sentences = Utils.randomizeOrder(sentences);

        BinaryParagraphWriter wr=new BinaryParagraphWriter();
        
        int wordsCount=0;
        for (P p : sentences) {
            byte[] pa = wr.write(p);
            int c = lucene.addSentence(p, pa);
            wordsCount += c;
        }
        return wordsCount;
    }
}
