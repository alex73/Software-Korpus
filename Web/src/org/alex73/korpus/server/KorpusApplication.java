package org.alex73.korpus.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.base.DBTagsGroups.KeyValue;
import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarFinder;
import org.alex73.korpus.base.TagLetter;
import org.alex73.korpus.server.data.GrammarInitial;
import org.alex73.korpus.server.data.GrammarInitial.GrammarLetter;
import org.alex73.korpus.server.data.InitialData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ApplicationPath("rest")
public class KorpusApplication extends Application {
    static final Logger LOGGER = LogManager.getLogger(KorpusApplication.class);

    public String korpusDir = System.getProperty("KORPUS_DIR");
    public String configDir = System.getProperty("CONFIG_DIR");

    Properties settings;
    Properties stat;
    GrammarDB2 gr;
    GrammarFinder grFinder;
    GrammarInitial grammarInitial;
    InitialData searchInitial;

    LuceneFilter processKorpus;
    LuceneFilter processOther;

    static KorpusApplication instance;

    public KorpusApplication() {
        instance = this;

        LOGGER.info("Starting...");
        try {
            InitialContext context = new InitialContext();
            Context xmlNode = (Context) context.lookup("java:comp/env");
            if (korpusDir == null) {
                korpusDir = (String) xmlNode.lookup("KORPUS_DIR");
            }
            if (configDir == null) {
                configDir = (String) xmlNode.lookup("CONFIG_DIR");
            }
            if (configDir == null) {
                LOGGER.fatal("CONFIG_DIR is not defined");
                return;
            }
            if (korpusDir == null) {
                LOGGER.fatal("KORPUS_DIR is not defined");
                return;
            }
            settings = loadSettings(configDir + "/settings.ini");
            stat = loadSettings(korpusDir + "/Korpus-cache/stat.properties");

            gr = GrammarDB2.initializeFromDir(korpusDir + "/GrammarDB/");
            LOGGER.info("GrammarDB loaded with " + gr.getAllParadigms().size() + " paradigms. Used memory: " + getUsedMemory());
            grFinder = new GrammarFinder(gr);
            LOGGER.info("GrammarDB indexed. Used memory: " + getUsedMemory());
            processKorpus = new LuceneFilter(korpusDir + "/Korpus-cache/");
            //TODO processOther = new LuceneFilter(dirPrefix + "/Other-cache/");
            LOGGER.info("Lucene initialized");

            prepareInitial();
            LOGGER.info("Initialization finished. Used memory: " + getUsedMemory());
        } catch (Throwable ex) {
            LOGGER.error("startup", ex);
            throw new ExceptionInInitializerError(ex);
        }
       //packages("org.alex73.korpus.server");
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

        grammarInitial.skipGrammar = new TreeMap<>();
        for (String k : (Set<String>) (Set<?>) settings.keySet()) {
            if (k.startsWith("grammar.skip.")) {
                Set<String> vs = Arrays.stream(settings.getProperty(k).split(";")).map(s -> s.trim())
                        .collect(Collectors.toSet());
                grammarInitial.skipGrammar.put(k.charAt(13), vs);
            }
        }

        searchInitial = new InitialData();
        searchInitial.authors = Arrays.asList(stat.getProperty("authors").split(";"));
        searchInitial.subcorpuses = new ArrayList<>();
        for (String k : (Set<String>) (Set<?>) settings.keySet()) {
            if (k.startsWith("subcorpus.")) {
                searchInitial.subcorpuses.add(new KeyValue(k.substring(10), settings.getProperty(k)));
            }
        }
        loadStyleGenres();
        searchInitial.grammar = grammarInitial;
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.info("shutdown");
        try {
            if (processKorpus!=null) {
                processKorpus.close();
            }
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

    private Properties loadSettings(String path) throws IOException {
        Properties result = new Properties();
        try (BufferedReader input = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            result.load(input);
        }
        return result;
    }

    private void loadStyleGenres() throws IOException {
        searchInitial.styleGenresParts = new ArrayList<>();
        searchInitial.styleGenres = new TreeMap<>();
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("/styleGenres.txt"), StandardCharsets.UTF_8))) {
            String s;
            while ((s = rd.readLine()) != null) {
                s = s.trim();
                int p = s.indexOf('/');
                String gr = p < 0 ? s : s.substring(0, p);
                if (!searchInitial.styleGenresParts.contains(gr)) {
                    searchInitial.styleGenresParts.add(gr);
                    searchInitial.styleGenres.put(gr, new ArrayList<>());
                }
                String n = p < 0 ? s : s.substring(p + 1);
                searchInitial.styleGenres.get(gr).add(n);
            }
        }
    }
}
