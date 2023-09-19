package org.alex73.korpus.compiler;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

import org.alex73.grammardb.GrammarDB2;
import org.alex73.grammardb.GrammarFinder;
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
        FilesReader r1 = new FilesReader(INPUT, true, h1);
        r1.finish(60);
        h1.finish(1);
        long af = System.currentTimeMillis();
        LOG.info("1st pass time: " + ((af - be) / 1000) + "s");
        errorsList.clear();

        int textsCount = h1.textInfos.size();
        textPositionsBySourceFile = h1.calcTextsPositions(OUTPUT.resolve("texts.jsons.gz"));

        GrammarDB2 gr;
        if (grammarDbPath != null) {
            LOG.info("Loading GrammarDB...");
            gr = GrammarDB2.initializeFromDir(grammarDbPath);
        } else {
            LOG.warn("GrammarDB will not be loaded !!!");
            Thread.sleep(5000);
            gr = GrammarDB2.empty();
        }
        GrammarFinder grFinder = new GrammarFinder(gr);
        StaticGrammarFiller2 grFiller = new StaticGrammarFiller2(grFinder);

        LOG.info("2nd pass...");
        be = System.currentTimeMillis();
        ProcessLuceneWriter lucene = new ProcessLuceneWriter(h1.allLanguages, writeToLucene, cacheForProduction, OUTPUT, 8192);
        ProcessPrepareLucene prepareLucene = new ProcessPrepareLucene(lucene);
        ProcessStat stat = new ProcessStat(processStat, OUTPUT, grFinder);
        ProcessTexts t2 = new ProcessTexts(grFiller, prepareLucene, stat, textsCount);
        FilesReader r2 = new FilesReader(INPUT, false, t2);
        r2.finish(3 * 60);
        t2.finish(10);
        LOG.info("Finishing stats...");
        stat.finish(OUTPUT);
        prepareLucene.finish(1);
        LOG.info("Finishing lucene...");
        lucene.finish(60); // finish or shutdown ?
        af = System.currentTimeMillis();
        LOG.info("2st pass time: " + ((af - be) / 1000 / 60) + "min");

        LOG.info("Merging Lucene and words counts...");
        ProcessMergeAll m = new ProcessMergeAll();
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(OUTPUT.resolve("stat-freq.zip")), 1024 * 1024))) {
            if (writeToLucene) {
                m.accept(lucene.mergeIndexes());
            }
            stat.mergeToZip(zip, m);
            m.finish(60);
            stat.removeTemp();
        }

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
        public synchronized void reportError(String place, String error, Throwable ex) {
            String key = error + " in '" + place + "'";
            synchronized (errorsList) {
                errorsList.add(key);
            }
            System.err.println(key);
            if (ex != null) {
                ex.printStackTrace();
            }
        }
    };
}
