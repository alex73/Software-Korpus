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
import java.io.ByteArrayOutputStream;
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
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamResult;

import org.alex73.korpus.editor.core.structure.Line;
import org.alex73.korpus.parser.Splitter;
import org.alex73.korpus.parser.TEIParser;
import org.alex73.korpus.server.Settings;
import org.alex73.korpus.server.engine.LuceneDriverWrite;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import alex73.corpus.paradigm.P;
import alex73.corpus.paradigm.Part;
import alex73.corpus.paradigm.Text;

/**
 * Class for loading Other texts into searchable cache.
 */
public class OtherLoading {

    static LuceneDriverWrite lucene;

    static int statTexts, statWords;
    static Set<String> volumes = new TreeSet<>();

    static void processOther() throws Exception {
        File dir = new File("Other-cache/");
        FileUtils.deleteDirectory(dir);
        dir.mkdirs();
        lucene = new LuceneDriverWrite("Other-cache/");

        Properties stat = new Properties();
        int allStatTexts = 0, allStatWords = 0;
        statTexts = 0;
        statWords = 0;
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
            statTexts = 0;
            statWords = 0;
            loadZipPagesToOther(f);
            allStatTexts++;
            allStatWords += statWords;
        }
        stat.setProperty("texts", "" + allStatTexts);
        stat.setProperty("words", "" + allStatWords);

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
        System.out.println(statTexts + " files processed");
    }

    protected static void loadZipPagesToOther(File f) throws Exception {
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
                loadTextToCorpus("kamunikat.org", "http://" + f.getName(), text);
            }
        }
    }

    protected static void loadTextToCorpus(String volume, String textUrl, String data) throws Exception {
        volumes.add(volume);

        lucene.setOtherInfo(volume, textUrl);

        data = data.trim().replace('\n', ' ').replace('\r', ' ').replaceAll("\\s{2,}", " ");
        Line line = new Splitter(data).splitParagraph();

        Text text = TEIParser.constructXML(Arrays.asList(line));

        List<P> sentences = new ArrayList<>();
        for (Object o : text.getBody().getHeadOrPOrDiv()) {
            if (o instanceof P) {
                sentences.add((P) o);
            } else if (o instanceof Part) {
            }
        }
        sentences = Utils.randomizeOrder(sentences);

        Marshaller m = TEIParser.CONTEXT.createMarshaller();
        for (P p : sentences) {
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            if (Settings.GZIP_TEXT_XML) {
                try (GZIPOutputStream baz = new GZIPOutputStream(ba)) {
                    m.marshal(p, new StreamResult(baz));
                }
            } else {
                m.marshal(p, new StreamResult(ba));
            }
            int c = lucene.addSentence(p, ba.toByteArray());
            statWords += c;
        }
    }
}
