package org.alex73.korpus.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.base.DBTagsGroups.KeyValue;
import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarFinder;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.belarusian.BelarusianTags;
import org.alex73.korpus.belarusian.TagLetter;
import org.alex73.korpus.server.data.GrammarInitial;
import org.alex73.korpus.server.data.GrammarInitial.GrammarLetter;
import org.alex73.korpus.server.data.InitialData;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationPath("rest")
public class KorpusApplication extends Application {
    public String korpusCache;
    public String grammarDb;
    public String configDir;

    List<String> settings;
    Properties stat;
    private List<String> textInfos;
    public GrammarDB2 gr;
    public GrammarFinder grFinder;
    public GrammarInitial grammarInitial;
    InitialData searchInitial;
    public Map<String, Set<String>> authorsByLemmas;
    public List<Set<String>> authorsGroups;

    LuceneFilter processKorpus;

    public static KorpusApplication instance;

    public KorpusApplication() {
        instance = this;

        System.out.println("Starting...");
        try {
            InitialContext context = new InitialContext();
            Context xmlNode = (Context) context.lookup("java:comp/env");
            korpusCache = (String) xmlNode.lookup("KORPUS_CACHE");
            grammarDb = (String) xmlNode.lookup("GRAMMAR_DB");
            configDir = (String) xmlNode.lookup("CONFIG_DIR");
            if (configDir == null) {
                throw new Exception("CONFIG_DIR is not defined");
            }
            if (korpusCache == null) {
                throw new Exception("KORPUS_DIR is not defined");
            }
            if (grammarDb == null) {
                throw new Exception("GRAMMAR_DB is not defined");
            }
            settings = Files.readAllLines(Paths.get(configDir + "/settings.ini"));
            stat = loadSettings(korpusCache + "/stat.properties");
            readTextInfos();
            loadAuthorsGroups();

            if (!grammarDb.isEmpty()) {
                gr = GrammarDB2.initializeFromDir(grammarDb);
            } else {
                gr = GrammarDB2.empty();
            }
            System.out.println("GrammarDB loaded with " + gr.getAllParadigms().size() + " paradigms. Used memory: "
                    + getUsedMemory());
            grFinder = new GrammarFinder(gr);
            System.out.println("GrammarDB indexed. Used memory: " + getUsedMemory());
            processKorpus = new LuceneFilter(korpusCache);
            System.out.println("Lucene initialized");

            prepareInitialGrammar();
            prepareInitialKorpus();
            System.out.println("Initialization finished. Used memory: " + getUsedMemory());
        } catch (Throwable ex) {
            System.err.println("Startup error");
            ex.printStackTrace();
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

    void prepareInitialGrammar() throws Exception {
        grammarInitial = new GrammarInitial();
        grammarInitial.grammarTree = new TreeMap<>();
        grammarInitial.grammarTree = addGrammar(BelarusianTags.getInstance().getRoot());
        grammarInitial.grammarWordTypes = DBTagsGroups.wordTypes;
        grammarInitial.grammarWordTypesGroups = DBTagsGroups.tagGroupsByWordType;

        grammarInitial.skipGrammar = new TreeMap<>();
        for (String line : settings) {
            if (line.startsWith("grammar.skip.") && line.charAt(14) == '=') {
                char part = line.charAt(13);
                Set<String> vs = Arrays.stream(line.substring(15).split(";")).map(s -> s.trim())
                        .collect(Collectors.toSet());
                grammarInitial.skipGrammar.put(part, vs);
            }
        }
        grammarInitial.slouniki=new ArrayList<>();
        for(String d:Files.readAllLines(Paths.get(grammarDb+"/slouniki.list"))) {
            GrammarInitial.GrammarDict dict = new GrammarInitial.GrammarDict();
            int p = d.indexOf('=');
            if (p < 0 || !d.substring(0, p).matches("[a-z0-9]+")) {
                throw new Exception("Wrong dictionary name format: " + d);
            }
            dict.name = d.substring(0, p);
            dict.desc = d.substring(p + 1);
            grammarInitial.slouniki.add(dict);
        }
    }

    void prepareInitialKorpus() throws Exception {
        searchInitial = new InitialData();
        searchInitial.subcorpuses = new ArrayList<>();
        searchInitial.authors = new TreeMap<>();
        searchInitial.sources = new TreeMap<>();
        for (String line : settings) {
            int eq = line.indexOf('=');
            if (eq < 0) {
                throw new RuntimeException();
            }
            if (line.startsWith("subcorpus.")) {
                searchInitial.subcorpuses.add(new KeyValue(line.substring(10, eq), line.substring(eq + 1)));
            }
        }
        for (String key : (Set<String>) (Set<?>) stat.keySet()) {
            if (key.startsWith("authors.")) {
                String subcorpus = key.substring(8);
                String value = stat.getProperty(key);
                searchInitial.authors.put(subcorpus, Arrays.asList(value.split(";")));
            } else if (key.startsWith("sources.")) {
                String subcorpus = key.substring(8);
                String value = stat.getProperty(key);
                searchInitial.sources.put(subcorpus, Arrays.asList(value.split(";")));
            }
        }
        loadStyleGenres();
        searchInitial.grammar = grammarInitial;
    }

    protected void readTextInfos() throws Exception {
        textInfos = Files.readAllLines(Paths.get(korpusCache + "/texts.jsons"));

        authorsByLemmas = new HashMap<>();
        for (String s : Files.readAllLines(Paths.get(korpusCache + "/lemma-authors.list"))) {
            int p = s.indexOf('=');
            if (p < 0) {
                throw new Exception("Wrong line: " + s);
            }
            String lemma = s.substring(0, p);
            String[] authorsList = s.substring(p + 1).split(";");
            Set<String> authors = authorsByLemmas.get(lemma);
            if (authors == null) {
                authors = new HashSet<>();
                authorsByLemmas.put(lemma, authors);
            }
            for (String a : authorsList) {
                authors.add(a);
            }
        }
    }

    protected void loadAuthorsGroups() throws Exception {
        authorsGroups = new ArrayList<>();
        Set<String> currentGroup = new TreeSet<>(Collator.getInstance(new Locale("be")));
        for (String s : Files.readAllLines(Paths.get(configDir + "/authors-groups.list"))) {
            s = s.trim();
            if (s.isEmpty() && !currentGroup.isEmpty()) {
                authorsGroups.add(new TreeSet<>(currentGroup));
                currentGroup.clear();
            }
        }
        if (!currentGroup.isEmpty()) {
            authorsGroups.add(new TreeSet<>(currentGroup));
        }
    }

    public TextInfo getTextInfo(int pos) {
        try {
            return new ObjectMapper().readValue(textInfos.get(pos), TextInfo.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("shutdown");
        try {
            if (processKorpus != null) {
                processKorpus.close();
            }
        } catch (Exception ex) {
            System.err.println("shutdown");
            ex.printStackTrace();
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
