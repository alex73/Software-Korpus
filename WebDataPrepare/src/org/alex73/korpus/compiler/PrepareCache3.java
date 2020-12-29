package org.alex73.korpus.compiler;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarFinder;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.elements.Paragraph;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.utils.KorpusDateTime;
import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PrepareCache3 {
    public static final boolean writeToLucene = true;
    public static final Path INPUT = Paths.get("Korpus-texts/");
    static final Path OUTPUT = Paths.get("/home/alex/Korpus-cache/");
    static StaticGrammarFiller2 grFiller;
    static LuceneDriverWrite lucene;
    public static List<TextInfo> textInfos = Collections.synchronizedList(new ArrayList<>());
    public static Map<String, Integer> textPositionsBySourceFile;

    static StatProcessing textStat = new StatProcessing();
    static List<String> errorsList = new ArrayList<>();

    static volatile Exception exception;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "32");

        Thread.currentThread().setName("Main parsing thread");
        FileUtils.deleteDirectory(OUTPUT.toFile());

        // read texts and sort
        new FilesReader().run(INPUT, errors, true);
        System.out.println("1st pass finished");
        errorsList.clear();

        luceneOpen(OUTPUT);
        textPositionsBySourceFile = calcTextsPositions();

        System.out.println("Load GrammarDB... " + new Date());
        GrammarDB2 gr = GrammarDB2.initializeFromDir("GrammarDB");
        grFiller = new StaticGrammarFiller2(new GrammarFinder(gr));

        new FilesReader().run(INPUT, errors, false);
        System.out.println("Finishing... " + new Date());
        luceneClose();
        textStat.write(OUTPUT);

        Collections.sort(errorsList);
        Files.write(OUTPUT.resolve("errors.txt"), errorsList);
        if (exception != null) {
            exception.printStackTrace();
        }

        System.out.println("Finished " + new Date());
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
        if (textInfo.creationTime != null) {
            new KorpusDateTime(textInfo.creationTime);
        }
        if (textInfo.publicationTime != null) {
            new KorpusDateTime(textInfo.publicationTime);
        }

        if (lucene == null) {
            // 1st pass - just remember TextInfo
            textInfos.add(textInfo);
        } else {
            // 2nd pass
            Collections.shuffle(content);
            grFiller.fill(content);
            textStat.add(textInfo, content);
            lucene.addSentences(textInfo, content);
        }
    }

    static Map<String, Integer> calcTextsPositions() throws Exception {
        // sort
        Collections.sort(textInfos, new TextOrder());

        // remember positions
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < textInfos.size(); i++) {
            if (result.put(textInfos.get(i).sourceFilePath, i) != null) {
                throw new Exception("Text " + textInfos.get(i).sourceFilePath + " produced twice !!!");
            }
        }

        // write to json
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        List<String> list2 = textInfos.stream().map(ti -> {
            try {
                return objectMapper.writeValueAsString(ti);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toList());
        Files.write(OUTPUT.resolve("texts.jsons"), list2);

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
