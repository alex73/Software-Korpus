package org.alex73.korpus.server;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarFinder;

@ApplicationPath("rest")
public class ApplicationOther extends Application {
    public String korpusCache;
    public GrammarDB2 gr;
    public GrammarFinder grFinder;
    private List<String> settings;
    public String synthUrl;

    public Map<Character, Set<String>> skipGrammar;

    public static ApplicationOther instance;

    public ApplicationOther() {
        instance = this;

        System.out.println("Starting...");
        try {
            InitialContext context = new InitialContext();
            Context xmlNode = (Context) context.lookup("java:comp/env");
            String configDir = (String) xmlNode.lookup("CONFIG_DIR");
            if (configDir == null) {
                throw new Exception("CONFIG_DIR is not defined");
            }
            korpusCache = (String) xmlNode.lookup("KORPUS_CACHE");
            if (korpusCache == null) {
                throw new Exception("KORPUS_CACHE is not defined");
            }
            String grammarDb = (String) xmlNode.lookup("GRAMMAR_DB");
            if (grammarDb == null) {
                throw new Exception("GRAMMAR_DB is not defined");
            }
            synthUrl = (String) xmlNode.lookup("SYNTH_URL");
            if (synthUrl == null) {
                throw new Exception("SYNTH_URL is not defined");
            }
            if (!grammarDb.isEmpty()) {
                gr = GrammarDB2.initializeFromDir(grammarDb);
            } else {
                gr = GrammarDB2.empty();
            }
            settings = Files.readAllLines(Paths.get(configDir + "/settings.ini"));
            System.out.println("GrammarDB loaded with " + gr.getAllParadigms().size() + " paradigms. Used memory: " + getUsedMemory());
            grFinder = new GrammarFinder(gr);
            System.out.println("GrammarDB indexed. Used memory: " + getUsedMemory());
            prepareInitialGrammar();
        } catch (Throwable ex) {
            System.err.println("Startup error");
            ex.printStackTrace();
            throw new ExceptionInInitializerError(ex);
        }
    }

    void prepareInitialGrammar() throws Exception {
        skipGrammar = new TreeMap<>();
        for (String line : settings) {
            if (line.startsWith("grammar.skip.") && line.charAt(14) == '=') {
                char part = line.charAt(13);
                Set<String> vs = Arrays.stream(line.substring(15).split(";")).map(s -> s.trim()).collect(Collectors.toSet());
                skipGrammar.put(part, vs);
            }
        }
    }

    private String getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        runtime.gc();
        runtime.gc();
        return Math.round((runtime.totalMemory() - runtime.freeMemory()) / 1024.0 / 1024.0) + "mb";
    }
}
