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

package org.alex73.korpus.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import alex73.corpus.paradigm.Paradigm;
import alex73.corpus.paradigm.Wordlist;

/**
 * Lite grammar database.
 * 
 * This storage used only for web for read-only search. Editor should use bigger
 * grammar storage with additional functionality.
 */
public class GrammarDBLite {

    static final Logger LOGGER = LogManager.getLogger(GrammarDBLite.class);

    public static final Locale BEL = new Locale("be");

    public static final JAXBContext CONTEXT;
    public static final Schema schema;

    private static GrammarDBLite instance;

    List<LiteParadigm> allParadigms = new ArrayList<>();
    Map<String, String> optimization = new HashMap<>();

    static {
        try {
            CONTEXT = JAXBContext.newInstance(Wordlist.class.getPackage().getName());
            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            schema = factory.newSchema(GrammarDBLite.class.getResource("/xsd/Paradigm.xsd"));
        } catch (Exception ex) {
            LOGGER.fatal(ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static synchronized GrammarDBLite getInstance() {
        return instance;
    }

    public static synchronized void initializeFromDir(File dir) throws Exception {
        if (instance != null) {
            return;
        }
        instance = new GrammarDBLite(dir);
    }

    private GrammarDBLite(File dir) throws Exception {
        File[] xmlFiles = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile()
                        && (pathname.getName().endsWith(".xml") || pathname.getName().endsWith(".xml.gz"));
            }
        });
        for (int i = 0; i < xmlFiles.length; i++) {
            LOGGER.info("Load " + xmlFiles[i].getPath());
            addXMLFile(xmlFiles[i]);
        }
    }

    public synchronized void addXMLFile(File file) throws Exception {
        if (file.getName().equals("clusters.xml")) {
            return;
        }
        Unmarshaller unm = CONTEXT.createUnmarshaller();

        InputStream in;
        if (file.getName().endsWith(".gz")) {
            in = new BufferedInputStream(new GZIPInputStream(new FileInputStream(file), 16384), 65536);
        } else {
            in = new BufferedInputStream(new FileInputStream(file), 65536);
        }
        try {
            Wordlist words = (Wordlist) unm.unmarshal(in);
            for (Paradigm p : words.getParadigm()) {
                addParadigm(p);
            }
        } finally {
            in.close();
        }
        optimization.clear();
    }

    private void addParadigm(Paradigm op) {
        LiteParadigm p = new LiteParadigm();
        p.lemma = optimizeString(op.getLemma());
        p.tag = optimizeString(op.getTag());
        p.forms = new LiteForm[op.getForm().size()];
        for (int i = 0; i < p.forms.length; i++) {
            Paradigm.Form of = op.getForm().get(i);
            LiteForm f = new LiteForm();
            f.tag = optimizeString(of.getTag());
            f.value = optimizeString(of.getValue());
            p.forms[i] = f;
        }
        allParadigms.add(p);
    }

    String optimizeString(String s) {
        String r = optimization.get(s);
        if (r == null) {
            optimization.put(s, s);
            r = s;
        }
        return r;
    }

    public synchronized List<LiteParadigm> getAllParadigms() {
        return Collections.unmodifiableList(allParadigms);
    }
}
