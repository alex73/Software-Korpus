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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamResult;

import org.alex73.korpus.editor.core.GrammarDB;
import org.alex73.korpus.editor.core.structure.KorpusDocument;
import org.alex73.korpus.editor.parser.TEIParser;
import org.alex73.korpus.editor.parser.TextParser;
import org.alex73.korpus.server.engine.LuceneDriver;

import alex73.corpus.paradigm.P;
import alex73.corpus.paradigm.Part;
import alex73.corpus.paradigm.S;
import alex73.corpus.paradigm.TEI;
import alex73.corpus.paradigm.Text;

/**
 * Class for loading texts into searchable cache.
 */
public class KorpusLoading {

    public static JAXBContext CONTEXT;
    static {
        try {
            CONTEXT = JAXBContext.newInstance(TEI.class.getPackage().getName());
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
    static Random RANDOM = new Random();

    static LuceneDriver lucene;

    static int statTexts, statWords;
    static int textId;
    static Set<String> stylegenres = new TreeSet<>();
    static Set<String> authors = new TreeSet<>();

    public static void main(String[] args) throws Exception {
        Locale.setDefault(new Locale("be"));

        new File("Korpus-cache/").mkdirs();
        lucene = new LuceneDriver(new File("Korpus-cache/"), true);
        
        System.out.println("Load GrammarDB...");
        GrammarDB.initializeFromDir(new File("GrammarDB"), new GrammarDB.LoaderProgress() {
            public void setFilesCount(int count) {
            }
            public void beforeFileLoading(String file) {
                System.out.println("Load "+file);
            }
            public void afterFileLoading() {
            }
        });

        List<File> files = new ArrayList<>();
        findFiles(new File("Korpus-texts/"), files);
        int c = 0;
        for (File f : files) {
            System.out.println("loadFileToCorpus " + f + ": " + (++c) + "/" + files.size());
            loadFileToCorpus(f);
        }
        if (c == 0) {
            System.out.println("Няма тэкстаў ў Korpus-texts/");
            System.exit(1);
        }
        System.out.println("Optimize...");
        lucene.shutdown();

        String stylegenresstr = "";
        for (String s : stylegenres) {
            stylegenresstr += ";" + s;
        }
        String authorsstr = "";
        for (String s : authors) {
            authorsstr += ";" + s;
        }

        Properties stat = new Properties();
        stat.setProperty("texts", "" + statTexts);
        stat.setProperty("words", "" + statWords);
        if (!stylegenres.isEmpty()) {
            stat.setProperty("stylegenres", stylegenresstr.substring(1));
        }else {
            stat.setProperty("stylegenres", "");
        }
        if (!authors.isEmpty()) {
            stat.setProperty("authors", authorsstr.substring(1));
        }else {
            stat.setProperty("authors", "");
        }
        FileOutputStream o = new FileOutputStream("Korpus-cache/stat.properties");
        stat.store(o, null);
        o.close();
        System.out.println(statTexts + " files processed");
    }
    
    static void loadGrammarDb() throws Exception {
        GrammarDB.getInstance().addThemesFile(new File("GrammarDB/themes.txt"));
        File[] xmlFiles=new File("GrammarDB").listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile()&& pathname.getName().endsWith(".xml");
            }
        });
        for (int i = 0; i < xmlFiles.length; i++) {
            System.out.println("  "+xmlFiles[i]);
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
            }
        }
    }

    protected static void loadFileToCorpus(File f) throws Exception {
        statTexts++;
        textId++;

        KorpusDocument doc;
        if (f.getName().endsWith(".xml")) {
            doc = TEIParser.parseXML(f);
        } else {
            doc = TextParser.parseText(f, false);
        }
        if (doc.textInfo.styleGenre!=null) {
            stylegenres.add(doc.textInfo.styleGenre);
        }
        for(String a:doc.textInfo.authors) {
            authors.add(a);
        }
        Text text = TEIParser.constructXML(doc);

        Marshaller m = CONTEXT.createMarshaller();
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
	    String stylegenre="";
	    String[] authors=new String[0];
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
