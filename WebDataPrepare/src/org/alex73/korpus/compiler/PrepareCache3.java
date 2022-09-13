package org.alex73.korpus.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarFinder;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.compiler.parsers.AuthorsUtil;
import org.alex73.korpus.text.parser.IProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrepareCache3 {
    private static final Logger LOG = LoggerFactory.getLogger(PrepareCache3.class);

    public static Path INPUT;
    public static Path OUTPUT;
    public static String grammarDbPath;
    public static boolean cacheForProduction;
    public static boolean writeToLucene = true;
    public static boolean processStat = true;

    public static Map<String, Integer> textPositionsBySourceFile;

    static List<String> errorsList = new ArrayList<>();

    static volatile Exception exception;

    public static void main(String[] args) throws Exception {
        String input = getKey("input", args, "Input path");
        String output = getKey("output", args, "Output path");
        grammarDbPath = getKey("grammardb", args, "GrammarDB path");
        if (input == null) {
            throw new Exception("--input not defined");
        }
        if (output == null) {
            throw new Exception("--output not defined");
        }
        cacheForProduction = getBooleanKey("prod", args, false, "Merge indexes into one");
        writeToLucene = getBooleanKey("write", args, true, "Write to Lucene index");
        processStat = getBooleanKey("stat", args, true, "Collect statistics");

        INPUT = Paths.get(input);
        OUTPUT = Paths.get(output);

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
        });

        BaseParallelProcessor.startStat();
        try {
            run();
        } catch (Throwable ex) {
            LOG.error("Error in main execution", ex);
        }
        BaseParallelProcessor.stopStat();
    }

    static void run() throws Exception {
        if (Files.exists(OUTPUT)) {
            try (Stream<Path> files = Files.walk(OUTPUT)) {
                files.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }
        }
        Files.createDirectories(OUTPUT);

        // read texts and sort
        LOG.info("1st pass...");
        long be = System.currentTimeMillis();

        AuthorsUtil.init(INPUT);

        ProcessHeaders h1 = new ProcessHeaders();
        ProcessFileParser p1 = new ProcessFileParser();
        FilesReader r1 = new FilesReader(INPUT, true, p1);
        r1.finish(60);
        p1.finish(5);
        h1.finish(1);
        long af = System.currentTimeMillis();
        LOG.info("1st pass time: " + ((af - be) / 1000) + "s");
        errorsList.clear();

        Set<String> subcorpuses = ProcessHeaders.textInfos.stream().map(ti -> ti.subcorpus).distinct().collect(Collectors.toSet());
        int textsCount = ProcessHeaders.textInfos.size();
        textPositionsBySourceFile = ProcessHeaders.calcTextsPositions(OUTPUT.resolve("texts.jsons.gz"));

        GrammarDB2 gr;
        if (grammarDbPath != null) {
            LOG.info("Loading GrammarDB...");
            gr = GrammarDB2.initializeFromDir(grammarDbPath);
        } else {
            LOG.warn("GrammarDB will not be loaded !!!");
            Thread.sleep(5000);
            gr = GrammarDB2.empty();
        }
        StaticGrammarFiller2 grFiller = new StaticGrammarFiller2(new GrammarFinder(gr));

        LOG.info("2nd pass...");
        be = System.currentTimeMillis();
        ProcessStat stat = new ProcessStat(processStat, subcorpuses);
        ProcessLuceneWriter lucene = new ProcessLuceneWriter(writeToLucene, cacheForProduction, OUTPUT.toString(), 2048);
        ProcessPrepareLucene prepareLucene = new ProcessPrepareLucene(lucene);
        ProcessTexts t2 = new ProcessTexts(grFiller, prepareLucene, stat, textsCount);
        ProcessFileParser p2 = new ProcessFileParser();
        FilesReader r2 = new FilesReader(INPUT, false, p2);
        r2.finish(3 * 60);
        p2.finish(10);
        t2.finish(10);
        LOG.info("Finishing stats...");
        stat.finish(OUTPUT);
        prepareLucene.finish(1);
        LOG.info("Finishing lucene...");
        lucene.finish(30); // finish or shutdown ?
        af = System.currentTimeMillis();
        LOG.info("2st pass time: " + ((af - be) / 1000 / 60) + "min");

        Collections.sort(errorsList);
        Files.write(OUTPUT.resolve("errors.txt"), errorsList);
        if (exception != null) {
            exception.printStackTrace();
        }

        LOG.info("Finished");
    }

    static String getKey(String key, String[] args, String description) {
        for (String a : args) {
            if (a.startsWith("--" + key + "=")) {
                String r = a.substring(key.length() + 3);
                LOG.info(description + ": " + r);
                return r;
            }
        }
        LOG.info(description + ": <not defined>");
        return null;
    }

    static boolean getBooleanKey(String key, String[] args, boolean def, String description) {
        for (String a : args) {
            if (a.startsWith("--" + key + "=")) {
                String v = a.substring(key.length() + 3);
                boolean r;
                switch (v.toLowerCase()) {
                case "true":
                    r = true;
                    break;
                case "false":
                    r = false;
                    break;
                default:
                    throw new RuntimeException("Wrong value: " + a);
                }
                LOG.info(description + ": " + r);
                return r;
            }
        }
        LOG.info(description + ": <not defined>, default value: " + def);
        return def;
    }

    public static IProcess errors = new IProcess() {
        @Override
        public synchronized void showStatus(String status) {
            System.out.println(status);
        }

        @Override
        public synchronized void reportError(String error, Throwable ex) {
            String key = ex == null ? error : error + ": " + ex.getMessage();
            synchronized (errorsList) {
                errorsList.add(key);
            }
            System.err.println(error);
            if (ex != null) {
                ex.printStackTrace();
            }
        }
    };
}
