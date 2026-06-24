package org.alex73.korpus.server;

import io.javalin.config.RoutesConfig;
import org.alex73.grammardb.GrammarDB2;
import org.alex73.grammardb.GrammarFinder;
import org.alex73.korpus.future.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ApplicationOther {
    private final GrammarDB2 grammarDB;
    private final GrammarFinder grFinder;
    private final String cachePath;
    private final List<String> settings;
    private final  String synthUrl;
    private final  Map<Character, Set<String>> skipGrammar;

    public ApplicationOther(String configDir, String cachePath, String synthUrl, GrammarDB2 grammarDB, GrammarFinder grFinder) throws Exception {
        this.grammarDB = grammarDB;
        this.grFinder = grFinder;
        this.cachePath = cachePath;
        this.synthUrl = synthUrl;
    /*
        String grammarDb = getEnvOrPropOrJndi("GRAMMAR_DB");
        if (grammarDb == null) {
            throw new Exception("GRAMMAR_DB is not defined");
        }
        synthUrl = getEnvOrPropOrJndi("SYNTH_URL");
        if (synthUrl == null) {
            throw new Exception("SYNTH_URL is not defined");
        }
        synthBookDir = getEnvOrPropOrJndi("SYNTH_BOOK_DIR");
        if (synthBookDir == null) {
            throw new Exception("SYNTH_BOOK_DIR is not defined");
        }
        synthBookUrl = getEnvOrPropOrJndi("SYNTH_BOOK_URL");
        if (synthBookUrl == null) {
            throw new Exception("SYNTH_BOOK_URL is not defined");
        }
        synthBookPrefix = getEnvOrPropOrJndi("SYNTH_BOOK_PREFIX");
        if (synthBookPrefix == null) {
            throw new Exception("SYNTH_BOOK_PREFIX is not defined");
        } */
        settings = Files.readAllLines(Paths.get(configDir + "/settings.ini"));

        skipGrammar=  prepareInitialGrammar();
    }


    public void registerRoutes(String prefix, RoutesConfig routes) {
//        routes.before(ctx -> {
//            ctx.header("Access-Control-Allow-Origin", "*");
//            ctx.header("Access-Control-Allow-Headers", "*");
//            ctx.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
//        });
//        routes.options("/*", ctx -> {
//            ctx.status(200);
//        });

        routes.get(prefix + "/freq/{count}", new Freq(cachePath));
        routes.get(prefix + "/arfa/{word}", new Arfa(grammarDB));
        routes.get(prefix + "/hramatycny/{word}", new Hramatycny(grammarDB, skipGrammar));
        routes.get(prefix + "/fanietycny/{word}", new Fanietycny(grammarDB, grFinder));
        routes.get(prefix + "/advarotny/{word}", new Advarotny(grammarDB));
        routes.get(prefix + "/amonimy/{type}/{level}", new Amonimy(grammarDB, grFinder));

        Kanvertary conv = new Kanvertary(grFinder, null, null, synthUrl);
        routes.post(prefix + "/rest/conv/fanetyka", ctx -> {
            ctx.contentType("text/html; charset=UTF-8").result(conv.fanetykaLog(ctx.body()));
        });
        routes.post(prefix + "/rest/conv/naciski", ctx -> {
            ctx.contentType("text/plain; charset=UTF-8").result(conv.naciski(ctx.body()));
        });
        routes.post(prefix + "/rest/conv/synth", ctx -> {
            String text = ctx.body();
            if (text.isBlank()) {
                ctx.status(400).result("Пусты тэкст");
                return;
            }
//            if (text.trim().startsWith("")) {
//                String r = conv.processBook(text.trim().substring(synthBookPrefix.length()).trim());
//                ctx.contentType("text/plain; charset=UTF-8").result(r);
//                return;
//            }
            if (text.length() > 2000) {
                text = text.substring(0, 2000);
            }

            conv.synth(text, ctx);
        });
    }

    Map<Character, Set<String>> prepareInitialGrammar() throws Exception {
        Map<Character, Set<String>> result = new TreeMap<>();
        for (String line : settings) {
            if (line.startsWith("grammar.skip.") && line.charAt(14) == '=') {
                char part = line.charAt(13);
                Set<String> vs = Arrays.stream(line.substring(15).split(";")).map(s -> s.trim()).collect(Collectors.toSet());
                result.put(part, vs);
            }
        }
        return result;
    }
}
