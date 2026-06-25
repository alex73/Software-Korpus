package org.alex73.korpus.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.config.RoutesConfig;
import org.alex73.grammardb.GrammarDB2;
import org.alex73.grammardb.GrammarFinder;
import org.alex73.grammardb.SetUtils;
import org.alex73.grammardb.structures.Variant;
import org.alex73.grammardb.tags.IGrammarTags;
import org.alex73.grammardb.tags.TagLetter;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.languages.DBTagsFactory.KeyValue;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.server.data.ClusterParams;
import org.alex73.korpus.server.data.GrammarInitial;
import org.alex73.korpus.server.data.GrammarInitial.GrammarLetter;
import org.alex73.korpus.server.data.InitialData;
import org.alex73.korpus.server.engine.TextInfos;
import org.alex73.korpus.shared.LemmaInfo;
import org.alex73.korpus.utils.KorpusFileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ApplicationKorpus {
    private final String configDir;
    public final String korpusCachePath;
    private final String grammarDbDir;
    public final String[] languages;
    public final GrammarDB2 grammarDb;
    public final GrammarFinder grFinder;

    private List<String> settings;
    private Properties stat;
    public final TextInfos infos;

    public StaticGrammarFiller2 grFiller;
    public GrammarInitial grammarInitial;
    InitialData searchInitial;
    public List<LemmaInfo.Author> authors;
    private Set<String> blacklistedAuthors;

    public LuceneFilter processKorpus;

    public ApplicationKorpus(String configDir, String cachePath, String languages, String grammarDbDir, GrammarDB2 grammarDb, GrammarFinder grFinder) throws Exception {
        this.configDir = configDir;
        this.korpusCachePath = cachePath;
        this.languages = languages.split(";");
        this.grammarDbDir = grammarDbDir;
        this.grammarDb = grammarDb;
        this.grFinder = grFinder;
        settings = Files.readAllLines(Paths.get(configDir + "/settings.ini"));
        stat = loadSettings(this.korpusCachePath + "/stat.properties");
        infos = new TextInfos(this.korpusCachePath + "/info.mapdb");
        loadAuthors(configDir);

        grFiller = new StaticGrammarFiller2(grFinder);
        processKorpus = new LuceneFilter(this.korpusCachePath, this.languages);
        System.out.println("Lucene initialized for languages: " + Arrays.toString(this.languages));

        prepareInitialGrammar();
        prepareInitialKorpus();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                processKorpus.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    public void registerRoutes(String prefix, RoutesConfig routes) {
        SearchServiceImpl searchService = new SearchServiceImpl(this);
        GrammarServiceImpl grammarService = new GrammarServiceImpl(this);


//            config.router.apiBuilder(() -> {
//                // Register routes
//            });


//            routes.before(ctx -> {
//                ctx.header("Access-Control-Allow-Origin", "*");
//                ctx.header("Access-Control-Allow-Headers", "*");
//                ctx.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
//            });
//            routes.options("/*", ctx -> {
//                ctx.status(200);
//            });

        // SearchServiceImpl routes
        routes.get(prefix + "/korpus/initial", ctx -> {
            ctx.json(searchService.getInitialData(ctx.ip()));
        });

        routes.get(prefix + "/korpus/freq/{subcorpus}", ctx -> {
            String subcorpus = ctx.pathParam("subcorpus");
            ctx.json(searchService.getFrequences(subcorpus, ctx.ip()));
        });

        routes.post(prefix + "/korpus/search", ctx -> {
            SearchServiceImpl.SearchRequest rq = ctx.bodyAsClass(SearchServiceImpl.SearchRequest.class);
            ctx.json(searchService.search(rq, ctx.ip()));
        });

        routes.post(prefix + "/korpus/searchTotalCount", ctx -> {
            SearchServiceImpl.SearchRequest rq = ctx.bodyAsClass(SearchServiceImpl.SearchRequest.class);
            ctx.json(searchService.searchTotalCount(rq, ctx.ip()));
        });

        routes.post(prefix + "/korpus/cluster", ctx -> {
            ClusterParams rq = ctx.bodyAsClass(ClusterParams.class);
            ctx.json(searchService.calculateClusters(rq, ctx.ip()));
        });

        routes.post(prefix + "/korpus/sentences", ctx -> {
            SearchServiceImpl.SentencesRequest rq = ctx.bodyAsClass(SearchServiceImpl.SentencesRequest.class);
            ctx.json(searchService.getSentences(rq));
        });


        // GrammarServiceImpl routes
        routes.get(prefix + "/grammar/initial", ctx -> {
            ctx.json(grammarService.getInitialData(ctx.ip()));
        });

        routes.post(prefix + "/grammar/search", ctx -> {
            GrammarServiceImpl.GrammarRequest rq = ctx.bodyAsClass(GrammarServiceImpl.GrammarRequest.class);
            ctx.json(grammarService.search(rq, ctx.ip()));
        });

        routes.get(prefix + "/grammar/details/{id}", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(grammarService.getLemmaDetails(id));
        });

        routes.get(prefix + "/grammar/detailsFull/{id}", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(grammarService.getLemmaFullDetails(id));
        });

        routes.get(prefix + "/grammar/lemmas/{form}", ctx -> {
            String form = ctx.pathParam("form");
            ctx.json(grammarService.getLemmasByForm(form));
        });
    }

    void prepareInitialGrammar() throws Exception {
        IGrammarTags tags = LanguageFactory.get("bel").getTags();
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
        for (String d : Files.readAllLines(Paths.get(grammarDbDir + "/slouniki.list"))) {
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
        grammarDb.getAllParadigms().parallelStream().forEach(p -> {
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
        searchInitial.languages = languages;
        searchInitial.subcorpuses = new ArrayList<>();
        searchInitial.authors = new TreeMap<>();
        searchInitial.sources = new TreeMap<>();
        searchInitial.showControls = new TreeMap<>();
        for (String line : settings) {
            if (line.isBlank() || line.startsWith("#")) {
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
            if (line.startsWith("show_controls.")) {
                String k = line.substring("show_controls.".length(), eq);
                String v = line.substring(eq + 1);
                searchInitial.showControls.put(k, v.split(","));
            }
        }
        for (String key : (Set<String>) (Set<?>) stat.keySet()) {
            if (key.startsWith("authors.")) {
                String subcorpus = key.substring(8);
                String value = stat.getProperty(key);
                List<String> subcorpusAuthors = new ArrayList<>(Arrays.asList(value.split(";")));
                subcorpusAuthors.removeAll(blacklistedAuthors);
                searchInitial.authors.put(subcorpus, subcorpusAuthors);
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

    protected void loadAuthors(String configDir) throws Exception {
        Set<String> lemmasAuthors = infos.authorsByLemmas.values().stream().flatMap(a -> a.stream()).collect(Collectors.toSet());
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
        for (String line : settings) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq < 0) {
                throw new RuntimeException();
            }
            String key = line.substring(0, eq).trim();
            if (!"authors.blacklist".equals(key)) {
                continue;
            }
            blacklistedAuthors = new HashSet<>();
            for (String a : line.substring(eq + 1).split(";")) {
                a = a.trim().replaceAll("\\s+", " ");
                blacklistedAuthors.add(a);
            }
        }
    }

    public Predicate<String[]> isAuthorsBlacklisted = authors -> {
        if (authors == null) {
            return false;
        }
        for (String a : authors) {
            if (blacklistedAuthors.contains(a)) {
                return true;
            }
        }
        return false;
    };

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
                new InputStreamReader(ApplicationKorpus.class.getResourceAsStream("/styleGenres.txt"), StandardCharsets.UTF_8))) {
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
