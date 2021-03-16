package org.alex73.korpus.compiler;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarFinder;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.elements.Paragraph;
import org.alex73.korpus.text.parser.IProcess;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PrepareCache3 {
    private static final Logger LOG = LoggerFactory.getLogger(PrepareCache3.class);

    public static final boolean writeToLucene = true;
    public static Path INPUT;
    public static Path OUTPUT;
    static StaticGrammarFiller2 grFiller;
    static LuceneDriverWrite lucene;
    public static List<TextInfo> textInfos = Collections.synchronizedList(new ArrayList<>());
    public static Map<String, Integer> textPositionsBySourceFile;

    static StatProcessing textStat = new StatProcessing();
    static List<String> errorsList = new ArrayList<>();

    static volatile Exception exception;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "32");

        String input = getKey("input", args);
        String output = getKey("output", args);
        String grammardb = getKey("grammardb", args);
        if (input == null) {
            throw new Exception("--input not defined");
        }
        if (output == null) {
            throw new Exception("--output not defined");
        }

        INPUT = Paths.get(input);
        OUTPUT = Paths.get(output);
        FileUtils.deleteDirectory(OUTPUT.toFile());

        // read texts and sort
        LOG.info("1st pass...");
        new FilesReader().run(INPUT, errors, true);
        errorsList.clear();

        luceneOpen(OUTPUT);
        textPositionsBySourceFile = calcTextsPositions();

        GrammarDB2 gr;
        if (grammardb != null) {
            LOG.info("Loading GrammarDB...");
            gr = GrammarDB2.initializeFromDir(grammardb);
        } else {
            gr = GrammarDB2.empty();
        }
        grFiller = new StaticGrammarFiller2(new GrammarFinder(gr));

        LOG.info("2nd pass...");
        new FilesReader().run(INPUT, errors, false);
        LOG.info("Finishing...");
        luceneClose();
        textStat.write(OUTPUT);

        Collections.sort(errorsList);
        Files.write(OUTPUT.resolve("errors.txt"), errorsList);
        if (exception != null) {
            exception.printStackTrace();
        }

        LOG.info("Finished");
    }

    static String getKey(String key, String[] args) {
        for (String a : args) {
            if (a.startsWith("--" + key + "=")) {
                return a.substring(key.length() + 3);
            }
        }
        return null;
    }

    static void luceneOpen(Path dir) throws Exception {
        FileUtils.deleteDirectory(dir.toFile());
        lucene = new LuceneDriverWrite(dir.toString());
    }

    static void luceneClose() throws Exception {
        lucene.shutdown();
        lucene = null;
    }

    public static void process(TextInfo textInfo, List<Paragraph> content) throws Exception {
        if (textInfo.sourceFilePath == null) {
            throw new RuntimeException("sourceFilePath нявызначаны");
        }
        if (textInfo.subcorpus == null) {
            throw new RuntimeException("subcorpus нявызначаны ў " + textInfo.sourceFilePath);
        }
        if (textInfo.title == null) {
            throw new RuntimeException("title нявызначаны ў " + textInfo.sourceFilePath);
        }
        textInfo.creationTimeLatest();
        textInfo.publicationTimeLatest();

        if (lucene == null) {
            // 1st pass - just remember TextInfo
            textInfos.add(textInfo);
        } else {
            // 2nd pass
            Collections.shuffle(content);
            grFiller.fill(content);
            textStat.add(textInfo, content);
            if (writeToLucene) {
                lucene.addSentences(textInfo, content);
            }
        }
    }

    static Map<String, Integer> calcTextsPositions() throws Exception {
        LOG.info("Sorting {} text infos...", textInfos.size());
        // sort
        Collections.sort(textInfos, new TextOrder());

        LOG.info("Storing text positions...");
        // remember positions
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < textInfos.size(); i++) {
            if (result.put(textInfos.get(i).sourceFilePath, i) != null) {
                throw new Exception("Text " + textInfos.get(i).sourceFilePath + " produced twice !!!");
            }
        }

        LOG.info("Writing texts infos to file...");
        // write to json
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        System.out.println(textInfos.size());
        try (BufferedWriter wr = Files.newBufferedWriter(OUTPUT.resolve("texts.jsons"))) {
            for (TextInfo ti : textInfos) {
                wr.write(objectMapper.writeValueAsString(ti));
                wr.write('\n');
            }
        }

        textInfos = null;

        return result;
    }

    static void writeStat(String dir, Properties stat) throws Exception {
        FileOutputStream o = new FileOutputStream(dir + "/stat.properties");
        stat.store(o, null);
        o.close();
    }

    public static IProcess errors = new IProcess() {
        @Override
        public synchronized void showStatus(String status) {
            // System.out.println(status);
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
