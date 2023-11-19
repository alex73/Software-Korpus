package org.alex73.korpus.compiler;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.alex73.grammardb.GrammarDB2;
import org.alex73.grammardb.GrammarFinder;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.utils.KorpusFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class PrepareCache4 {
    private static final Logger LOG = LoggerFactory.getLogger(PrepareCache4.class);

    public static Path INPUT;
    public static Path OUTPUT;
    public static String grammarDbPath;
    public static String[] languages;
    public static boolean cacheForProduction;
    public static boolean writeToLucene = true;
    public static boolean processStat = true;

    static int cores;
    static GrammarFinder grFinder;
    static StaticGrammarFiller2 grFiller;

    static List<TextInfo> textInfos = new ArrayList<>();
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
        String langs = getKey("languages", args, "Languages list separated by comma");
        if (langs == null) {
            throw new Exception("--languages not defined");
        }
        languages = langs.split(",");

        cacheForProduction = getBooleanKey("prod", args, false, "Merge indexes into one");
        writeToLucene = getBooleanKey("write", args, true, "Write to Lucene index");
        processStat = getBooleanKey("stat", args, true, "Collect statistics");

        INPUT = Paths.get(input);
        OUTPUT = Paths.get(output);

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
        });

        emptyDir(OUTPUT);
        loadGrammarDB();

        cores = Runtime.getRuntime().availableProcessors();

        Step1Split.init(languages, errors);
        Step2Grammar.init(grFiller);
        Step3Stat.init(OUTPUT, grFinder);
        Step5WriteLucene.init(cores, languages, writeToLucene, cacheForProduction, OUTPUT, 2 * 1024);

        LOG.info("Process...");
        long be = System.currentTimeMillis();
        // ProcessLuceneWriter lucene = new ProcessLuceneWriter(languages,
        // writeToLucene, cacheForProduction, OUTPUT, 8192);
        // ProcessPrepareLucene prepareLucene = new ProcessPrepareLucene(lucene);
        // ProcessStat stat = new ProcessStat(processStat, OUTPUT, grFinder);
        // ProcessTexts t2 = new ProcessTexts(grFiller, prepareLucene, stat,
        // textsCount);
        parseZips();

        writeTextHeaders();

        LOG.info("Finishing stats...");
        Step3Stat.finish(OUTPUT);
        LOG.info("Finishing lucene...");
        Step5WriteLucene.finish(); // finish or shutdown ?
        long af = System.currentTimeMillis();
        LOG.info("Process time: " + ((af - be) / 1000 / 60) + "min");

        LOG.info("Merging Lucene and words counts...");
        if (writeToLucene) {
            Step5WriteLucene.mergeIndexes();
        }
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(OUTPUT.resolve("stat-freq.zip")), 1024 * 1024))) {
            Step3Stat.mergeToZip(zip);
            Step3Stat.removeTemp();
        }

        Collections.sort(errorsList);
        Files.write(OUTPUT.resolve("errors.txt"), errorsList);
        if (exception != null) {
            exception.printStackTrace();
        }

        LOG.info("Finished");
    }

    static void emptyDir(Path dir) throws Exception {
        if (Files.exists(dir)) {
            try (Stream<Path> files = Files.walk(dir)) {
                files.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }
        }
        Files.createDirectories(dir);
    }

    static void loadGrammarDB() throws Exception {
        GrammarDB2 gr;
        if (grammarDbPath != null) {
            LOG.info("Loading GrammarDB...");
            gr = GrammarDB2.initializeFromDir(grammarDbPath);
        } else {
            LOG.warn("GrammarDB will not be loaded !!!");
            Thread.sleep(1000);
            gr = GrammarDB2.empty();
        }
        grFinder = new GrammarFinder(gr);
        grFiller = new StaticGrammarFiller2(grFinder);
    }

    static void parseZips() throws Exception {
        List<Path> files = Files.list(INPUT).filter(p -> p.toString().endsWith(".zip")).sorted().toList();
        for (Path file : files) {
            LOG.info("Read zip " + file);
            long before = System.currentTimeMillis();

            int totalFilesInZip = 0;
            try (ZipFile zipFile = new ZipFile(file.toFile())) {
                for (Enumeration<? extends ZipEntry> en = zipFile.entries(); en.hasMoreElements(); en.nextElement()) {
                    totalFilesInZip++;
                }
            }

            try (ExecutorService executor = new ThreadPoolExecutor(cores, cores, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(cores * 2),
                    new WaitPolicy(1, TimeUnit.HOURS))) {

                long prevDumpTime = System.currentTimeMillis();
                int processedFiles = 0;
                String subcorpus = file.getFileName().toString().replaceAll("^[0-9]+\\.", "").replaceAll("\\.zip$", "");
                try (ZipInputStream in = new ZipInputStream(Files.newInputStream(file))) {
                    ZipEntry ze = null;
                    while ((ze = in.getNextEntry()) != null) {
                        byte[] data = in.readAllBytes();
                        String fn = file.getFileName() + "!" + ze.getName();
                        int gto = textInfos.size();
                        textInfos.add(null);
                        executor.execute(() -> {
                            processText(subcorpus, gto, fn, data);
                        });
                        processedFiles++;
                        if (prevDumpTime + 60000 < System.currentTimeMillis()) {
                            prevDumpTime = System.currentTimeMillis();
                            LOG.info("  processed " + processedFiles + " files from " + totalFilesInZip);
                        }
                    }
                }
                executor.shutdown();
                executor.awaitTermination(1, TimeUnit.HOURS);
            }
            Step3Stat.flush();

            long after = System.currentTimeMillis();
            LOG.info("Finished zip " + file + ", processing time: " + (after - before) / 1000 + "s");
        }
    }

    static void processText(String subcorpus, int globalTextOrder, String fn, byte[] data) {
        try {
            MessageParsedText outText = Step1Split.run(data, subcorpus);
            textInfos.set(globalTextOrder, outText.textInfo);
            outText.textInfo.subcorpus = subcorpus;
            outText.textInfo.globalTextOrder = globalTextOrder;
            Step2Grammar.run(outText);
            if (processStat) {
                Step3Stat.run(outText);
            }
            MessageLuceneWrite outLucene = Step4PrepareLucene.run(outText);
            Step5WriteLucene.run(outLucene);
        } catch (Exception ex) {
            errors.reportError(fn, ex.getMessage(), ex);
        }
    }

    static void writeTextHeaders() throws Exception {
        Gson gson = new Gson();
        KorpusFileUtils.writeGzip(OUTPUT.resolve("texts.jsons.gz"), textInfos.stream().map(ti -> {
            try {
                return gson.toJson(ti);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }));
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

    static class WaitPolicy implements RejectedExecutionHandler {
        private final long timeout;
        private final TimeUnit unit;

        public WaitPolicy(long timeout, TimeUnit unit) {
            this.timeout = timeout;
            this.unit = unit;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                try {
                    BlockingQueue<Runnable> queue = executor.getQueue();
                    if (!queue.offer(r, timeout, unit)) {
                        throw new RejectedExecutionException("Max wait time expired to queue task");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RejectedExecutionException("Interrupted", e);
                }
            } else {
                throw new RejectedExecutionException("Executor has been shut down");
            }
        }
    }
}
