package org.alex73.korpus.server;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.annotation.PreDestroy;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarFinder;
import org.alex73.korpus.base.TagLetter;
import org.alex73.korpus.server.data.GrammarInitial;
import org.alex73.korpus.server.data.GrammarInitial.GrammarLetter;
import org.alex73.korpus.server.data.InitialData;
import org.alex73.korpus.shared.StyleGenres;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ApplicationPath("/rest")
public class KorpusApplication extends Application {
    static final Logger LOGGER = LogManager.getLogger(KorpusApplication.class);

    public String dirPrefix = System.getProperty("KORPUS_DIR");

    GrammarDB2 gr;
    GrammarFinder grFinder;
    GrammarInitial grammarInitial;
    InitialData searchInitial;

    LuceneFilter processKorpus;
    LuceneFilter processOther;

    static KorpusApplication instance;

    public KorpusApplication() {
        instance = this;
        if (dirPrefix == null) {
            LOGGER.fatal("KORPUS_DIR is not defined");
            return;
        }
        LOGGER.info("Starting...");
        try {
            gr = GrammarDB2.initializeFromDir(dirPrefix + "/GrammarDB/");
            LOGGER.info("GrammarDB loaded with " + gr.getAllParadigms().size() + " paradigms. Used memory: " + getUsedMemory());
            grFinder = new GrammarFinder(gr);
            LOGGER.info("GrammarDB indexed with " + grFinder.size() + " forms. Used memory: " + getUsedMemory());
            processKorpus = new LuceneFilter(dirPrefix + "/Korpus-cache/");
            processOther = new LuceneFilter(dirPrefix + "/Other-cache/");
            LOGGER.info("Lucene initialized");

            prepareInitial();
            LOGGER.info("Initialization finished. Used memory: " + getUsedMemory());
        } catch (Throwable ex) {
            LOGGER.error("startup", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    private String getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        runtime.gc();
        runtime.gc();
        return Math.round((runtime.totalMemory() - runtime.freeMemory()) / 1024.0 / 1024.0) + "mb";
    }

    void prepareInitial() throws Exception {
        grammarInitial = new GrammarInitial();
        grammarInitial.grammarTree = new TreeMap<>();
        grammarInitial.grammarTree = addGrammar(BelarusianTags.getInstance().getRoot());
        grammarInitial.grammarWordTypes = DBTagsGroups.wordTypes;
        grammarInitial.grammarWordTypesGroups = DBTagsGroups.tagGroupsByWordType;

        searchInitial = new InitialData();

        Properties props = new Properties();
        try (InputStream in = new FileInputStream(dirPrefix + "/Korpus-cache/stat.properties")) {
            props.load(in);
        }
        searchInitial.authors = Arrays.asList(props.getProperty("authors").split(";"));
        searchInitial.statKorpus = stat(props);
        searchInitial.styleGenresParts = StyleGenres.KNOWN_GROUPS;
        searchInitial.styleGenres = new TreeMap<>();
        for (String p : StyleGenres.KNOWN_GROUPS) {
            searchInitial.styleGenres.put(p, new ArrayList<>());
        }
        for (String k : StyleGenres.KNOWN) {
            String[] p = k.split("/");
            if (p.length == 2) {
                searchInitial.styleGenres.get(p[0]).add(p[1]);
            }
        }
        searchInitial.grammar = grammarInitial;

        props = new Properties();
        try (InputStream in = new FileInputStream(dirPrefix + "/Other-cache/stat.properties")) {
            props.load(in);
        }
        searchInitial.volumes = Arrays.asList(props.getProperty("volumes").split(";"));
        searchInitial.statOther = stat(props);
    }

    List<InitialData.StatLine> stat(Properties props) {
        List<String> names = new ArrayList<>();
        for (Object k : props.keySet()) {
            if (k.toString().startsWith("texts.")) {
                names.add(k.toString().substring(6));
            }
        }
        Collections.sort(names);
        if (names.contains("_")) {
            names.remove("_");
            names.add("_");
        }

        List<InitialData.StatLine> r = new ArrayList<>();
        InitialData.StatLine row0 = new InitialData.StatLine();
        row0.name = "Агулам";
        row0.texts = 0;
        row0.words = 0;
        r.add(row0);
        for (String n : names) {
            InitialData.StatLine row = new InitialData.StatLine();
            row.name = "_".equals(n) ? "нявызначаны" : n;
            row.texts = Integer.parseInt(props.getProperty("texts." + n));
            row.words = Integer.parseInt(props.getProperty("words." + n));
            row0.texts += row.texts;
            row0.words += row.words;
            r.add(row);
        }
        return r;
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.info("shutdown");
        try {
            processKorpus.close();
            processOther.close();
        } catch (Exception ex) {
            LOGGER.error("shutdown", ex);
        }
    }

    private Map<Character, GrammarLetter> addGrammar(TagLetter letters) {
        if (letters.letters.isEmpty()) {
            return null;
        }
        Map<Character, GrammarLetter> result = new TreeMap<>();
        for (TagLetter.OneLetterInfo lt : letters.letters) {
            GrammarLetter g = new GrammarLetter();
            g.name = lt.groupName;
            g.desc = lt.description;
            g.ch = addGrammar(lt.nextLetters);
            result.put(lt.letter, g);
        }
        return result;
    }
}
