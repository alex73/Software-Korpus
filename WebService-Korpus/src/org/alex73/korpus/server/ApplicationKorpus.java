package org.alex73.korpus.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarFinder;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.languages.DBTagsFactory.KeyValue;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.languages.TagLetter;
import org.alex73.korpus.server.data.GrammarInitial;
import org.alex73.korpus.server.data.GrammarInitial.GrammarLetter;
import org.alex73.korpus.server.data.InitialData;
import org.alex73.korpus.shared.LemmaInfo;
import org.alex73.korpus.utils.KorpusFileUtils;
import org.alex73.korpus.utils.SetUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationPath("/")
public class ApplicationKorpus extends Application {
    public String korpusCache;
    public String grammarDb;
    public String configDir;
    public String languages;

    private List<String> settings;
    private Properties stat;
    private List<String> textInfos;
    public GrammarDB2 gr;
    public GrammarFinder grFinder;
    public GrammarInitial grammarInitial;
    InitialData searchInitial;
    public Map<String, Set<String>> authorsByLemmas;
    public List<LemmaInfo.Author> authors;

    public LuceneFilter processKorpus;

    public static ApplicationKorpus instance;

    public ApplicationKorpus() {
        instance = this;

        System.out.println("Starting...");
        try {
            InitialContext context = new InitialContext();
            Context xmlNode = (Context) context.lookup("java:comp/env");
            korpusCache = (String) xmlNode.lookup("KORPUS_CACHE");
            grammarDb = (String) xmlNode.lookup("GRAMMAR_DB");
            configDir = (String) xmlNode.lookup("CONFIG_DIR");
            languages = (String) xmlNode.lookup("KORPUS_LANGUAGES");
            if (configDir == null) {
                throw new Exception("CONFIG_DIR is not defined");
            }
            if (korpusCache == null) {
                throw new Exception("KORPUS_CACHE is not defined");
            }
            if (grammarDb == null) {
                throw new Exception("GRAMMAR_DB is not defined");
            }
            if (languages == null) {
                throw new Exception("KORPUS_LANGUAGES is not defined");
            }
            settings = Files.readAllLines(Paths.get(configDir + "/settings.ini"));
            stat = loadSettings(korpusCache + "/stat.properties");
            readTextInfos();
            loadAuthors();

            if (!grammarDb.isEmpty()) {
                gr = GrammarDB2.initializeFromDir(grammarDb);
            } else {
                gr = GrammarDB2.empty();
            }
            System.out.println("GrammarDB loaded with " + gr.getAllParadigms().size() + " paradigms. Used memory: " + getUsedMemory());
            grFinder = new GrammarFinder(gr);
            System.out.println("GrammarDB indexed. Used memory: " + getUsedMemory());
            processKorpus = new LuceneFilter(korpusCache, languages.split(";"));
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
        ILanguage.IGrammarTags tags = LanguageFactory.get("bel").getTags();
        ILanguage.IDBTags dbtf = LanguageFactory.get("bel").getDbTags();
        grammarInitial = new GrammarInitial();
        grammarInitial.grammarTree = new TreeMap<>();
        grammarInitial.grammarTree = addGrammar(tags.getRoot());
        grammarInitial.grammarWordTypes = dbtf.getWordTypes();
        grammarInitial.grammarWordTypesGroups = dbtf.getTagGroupsByWordType();

        grammarInitial.skipGrammar = new TreeMap<>();
        for (String line : settings) {
            if (line.startsWith("grammar.skip.") && line.charAt(14) == '=') {
                char part = line.charAt(13);
                Set<String> vs = Arrays.stream(line.substring(15).split(";")).map(s -> s.trim()).collect(Collectors.toSet());
                grammarInitial.skipGrammar.put(part, vs);
            }
        }
        grammarInitial.slouniki = new ArrayList<>();
        for (String d : Files.readAllLines(Paths.get(grammarDb + "/slouniki.list"))) {
            GrammarInitial.GrammarDict dict = new GrammarInitial.GrammarDict();
            int p = d.indexOf('=');
            if (p < 0 || !d.substring(0, p).matches("[a-z0-9]+")) {
                throw new Exception("Wrong dictionary name format: " + d);
            }
            dict.name = d.substring(0, p);
            dict.desc = d.substring(p + 1);
            grammarInitial.slouniki.add(dict);
        }
        grammarInitial.stat = new ArrayList<>();
        GrammarInitial.Stat grStatTotal = new GrammarInitial.Stat();
        grammarInitial.stat.add(grStatTotal);
        Map<Character, GrammarInitial.Stat> grStats = new TreeMap<>();
        for (TagLetter.OneLetterInfo li : tags.getRoot().letters) {
            GrammarInitial.Stat gs = new GrammarInitial.Stat();
            gs.title = "&nbsp;&nbsp;&nbsp;&nbsp;" + li.description;
            grammarInitial.stat.add(gs);
            grStats.put(li.letter, gs);
        }
        gr.getAllParadigms().parallelStream().forEach(p -> {
            int formsInParadigm = p.getVariant().stream().mapToInt(v -> v.getForm().size()).sum();
            synchronized (grStatTotal) {
                grStatTotal.paradigmCount++;
                grStatTotal.formCount += formsInParadigm;
            }
            p.getVariant().stream().map(v -> SetUtils.tag(p, v).charAt(0)).sorted().distinct().forEach(c -> {
                GrammarInitial.Stat st = grStats.get(c);
                synchronized (st) {
                    st.paradigmCount++;
                }
            });
            for (Variant v : p.getVariant()) {
                char c = SetUtils.tag(p, v).charAt(0);
                GrammarInitial.Stat st = grStats.get(c);
                synchronized (st) {
                    st.formCount += v.getForm().size();
                }
            }
        });
    }

    void prepareInitialKorpus() throws Exception {
        searchInitial = new InitialData();
        searchInitial.subcorpuses = new ArrayList<>();
        searchInitial.authors = new TreeMap<>();
        searchInitial.sources = new TreeMap<>();
        for (String line : settings) {
            if (line.isBlank()) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq < 0) {
                throw new RuntimeException();
            }
            if (line.startsWith("subcorpus.")) {
                searchInitial.subcorpuses.add(new KeyValue(line.substring(10, eq), line.substring(eq + 1)));
            }
            if (line.startsWith("kankardansnyja_spisy=")) {
                searchInitial.kankardansnyjaSpisy = line.substring(21).trim().split(";");
            }
            if (line.startsWith("preselected_subcorpuses=")) {
                searchInitial.preselectedSubcorpuses = line.substring(24).trim();
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
        searchInitial.stat = new ArrayList<>();
        InitialData.Stat s = new InitialData.Stat();
        s.texts = Long.parseLong(stat.getProperty("texts.", "0"));
        s.words = Long.parseLong(stat.getProperty("words.", "0"));
        searchInitial.stat.add(s);
        for (KeyValue k : searchInitial.subcorpuses) {
            s = new InitialData.Stat();
            s.name = "&nbsp;&nbsp;&nbsp;&nbsp;" + k.value.replaceAll("\\|\\|.+", "");
            s.texts = Long.parseLong(stat.getProperty("texts." + k.key, "0"));
            s.words = Long.parseLong(stat.getProperty("words." + k.key, "0"));
            searchInitial.stat.add(s);
            switch (k.key) {
            case "teksty":
                searchInitial.styleGenresParts.stream().map(st -> st.replaceAll("/.+", "")).distinct().forEach(st -> {
                    InitialData.Stat s2 = new InitialData.Stat();
                    s2.name = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + st;
                    s2.texts = Integer.parseInt(stat.getProperty("texts." + k.key + "." + st, "0"));
                    s2.words = Integer.parseInt(stat.getProperty("words." + k.key + "." + st, "0"));
                    searchInitial.stat.add(s2);
                });
                break;
            case "sajty":
                Arrays.asList(stat.getProperty("sources.sajty", "").split(";")).forEach(sa -> {
                    InitialData.Stat s2 = new InitialData.Stat();
                    s2.name = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + sa;
                    s2.texts = Integer.parseInt(stat.getProperty("texts." + k.key + "." + sa, "0"));
                    s2.words = Integer.parseInt(stat.getProperty("words." + k.key + "." + sa, "0"));
                    searchInitial.stat.add(s2);
                });
                break;
            case "nierazabranaje":
                Arrays.asList(stat.getProperty("sources.nierazabranaje", "").split(";")).forEach(sa -> {
                    InitialData.Stat s2 = new InitialData.Stat();
                    s2.name = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + sa;
                    s2.texts = Integer.parseInt(stat.getProperty("texts." + k.key + "." + sa, "0"));
                    s2.words = Integer.parseInt(stat.getProperty("words." + k.key + "." + sa, "0"));
                    searchInitial.stat.add(s2);
                });
                break;
            }
        }
    }

    protected void readTextInfos() throws Exception {
        textInfos = KorpusFileUtils.readGzip(Paths.get(korpusCache + "/texts.jsons.gz"));

        authorsByLemmas = new HashMap<>();
        KorpusFileUtils.readGzip(Paths.get(korpusCache + "/lemma-authors.list.gz")).forEach(s -> {
            int p = s.indexOf('=');
            if (p < 0) {
                throw new RuntimeException("Wrong line: " + s);
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
        });
    }

    protected void loadAuthors() throws Exception {
        Set<String> lemmasAuthors = authorsByLemmas.values().stream().flatMap(a -> a.stream()).collect(Collectors.toSet());
        Set<String> uniqAuthors = new HashSet<>();
        authors = new ArrayList<>();
        for (String s : Files.readAllLines(Paths.get(configDir + "/authors-groups.list"))) {
            if (s.isBlank()) {
                continue;
            }
            int p = s.indexOf('=');
            if (p < 0) {
                throw new Exception("Wrong author: " + s);
            }
            LemmaInfo.Author a = new LemmaInfo.Author();
            a.name = s.substring(0, p).trim();
            a.displayName = s.substring(p + 1).trim();
            if (!uniqAuthors.add(a.displayName)) {
                throw new Exception("Author already defined: " + a.displayName);
            }
            if (!lemmasAuthors.contains(a.name)) {
                System.err.println("Author '" + a.name + "' not used in corpus");
            }
            authors.add(a);
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
        try (BufferedReader rd = new BufferedReader(
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("/styleGenres.txt"), StandardCharsets.UTF_8))) {
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
