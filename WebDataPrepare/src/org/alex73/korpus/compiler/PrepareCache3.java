package org.alex73.korpus.compiler;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.StaticGrammarFiller;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.Poetry;
import org.alex73.korpus.utils.KorpusDateTime;
import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PrepareCache3 {
    public static final Path INPUT = Paths.get("Korpus-texts/");
    static final Path OUTPUT = Paths.get("/home/alex/Korpus-cache/");
    static StaticGrammarFiller grFiller;
    static LuceneDriverWrite lucene;
    public static List<TextInfo> textInfos = Collections.synchronizedList(new ArrayList<>());
    public static Map<String, Integer> textPositionsBySourceFile;

    static StatProcessing textStat = new StatProcessing();
    static volatile Exception exception;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "32");

        Thread.currentThread().setName("Main parsing thread");
        FileUtils.deleteDirectory(OUTPUT.toFile());

        // read texts and sort
        new FilesReader().run(INPUT, errors);
        System.out.println("1st pass finished");
        errorsCount.clear();

        luceneOpen(OUTPUT);
        textPositionsBySourceFile = calcTextsPositions();

        System.out.println("Load GrammarDB... " + new Date());
        GrammarDB2 gr = GrammarDB2.initializeFromDir("GrammarDB");
        grFiller = new StaticGrammarFiller(gr);

        new FilesReader().run(INPUT, errors);
        System.out.println("Finishing... " + new Date());
        luceneClose();
        textStat.write(OUTPUT);

        List<String> errorNames = new ArrayList<>(errorsCount.keySet());
        Collections.sort(errorNames, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int c1 = errorsCount.get(o1);
                int c2 = errorsCount.get(o2);
                return c1 - c2;
            }
        });
        for (String e : errorNames) {
            System.err.println("ERROR: " + e + ": " + errorsCount.get(e));
        }
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

    public static void process(TextInfo textInfo, List<Object> content) throws Exception {
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

            List<P> ps = new ArrayList<>();
            content.forEach(op -> {
                if (op instanceof P) {
                    ps.add((P) op);
                } else if (op instanceof Poetry) {
                    ((Poetry) op).getPOrTag().forEach(op2 -> {
                        if (op2 instanceof P) {
                            ps.add((P) op2);
                        }
                    });
                }
            });
            lucene.addSentences(textInfo, ps);
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

    private static Map<String, Integer> errorsCount = new HashMap<>();
    public static IProcess errors = new IProcess() {
        @Override
        public synchronized void showStatus(String status) {
            // System.out.println(status);
        }

        @Override
        public synchronized void reportError(String error, Throwable ex) {
            Integer count = errorsCount.get(error);
            if (count == null) {
                count = 1;
            } else {
                count++;
            }
            errorsCount.put(error, count);
            System.err.println(error);
            if (ex != null) {
                ex.printStackTrace();
            }
        }
    };
}
