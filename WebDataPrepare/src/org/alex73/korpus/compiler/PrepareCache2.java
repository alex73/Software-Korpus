package org.alex73.korpus.compiler;

import java.io.FileOutputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarFiller2;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.parsers.IParser;
import org.alex73.korpus.compiler.parsers.ParserFactory;
import org.alex73.korpus.server.engine.LuceneDriverWrite;
import org.alex73.korpus.server.text.BinaryParagraphWriter;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.Poetry;
import org.alex73.korpus.text.xml.XMLText;
import org.apache.commons.io.FileUtils;

public class PrepareCache2 {
    static final Path INPUT = Paths.get("Korpus-texts/");
    static final Path OUTPUT = Paths.get("Korpus-cache/");
    static final Object WRITER_LOCK = new Object();
    static GrammarFiller2 grFiller;
    static LuceneDriverWrite lucene;

    static String currentSubcorpus;
    static StatProcessing textStat = new StatProcessing();
    static volatile Exception exception;
    

    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "32");

        Thread.currentThread().setName("Main parsing thread");
        System.out.println("Load GrammarDB...");
        GrammarDB2 gr = GrammarDB2.initializeFromDir("GrammarDB");
        grFiller = new GrammarFiller2(gr);

        // main texts corpus
        luceneOpen(OUTPUT);
        Files.find(INPUT, Integer.MAX_VALUE, (p, a) -> a.isRegularFile(), FileVisitOption.FOLLOW_LINKS).sorted()
                .forEach(p -> {
                    String rel = INPUT.relativize(p).toString();
                    currentSubcorpus = rel.substring(0,rel.indexOf('/'));
                    IParser parser = ParserFactory.getParser(rel);
                    if (parser == null) {
                        errors.reportError("Unknown parser for " + rel);
                    } else {
                        try {
                            parser.parse(p);
                        } catch (Exception ex) {
                            errors.reportError(ex.getMessage() + ": " + p);
                        }
                    }
                });
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
Files.write(Paths.get("log"), LuceneDriverWrite.log);
        System.out.println("Finished");
    }

    static void luceneOpen(Path dir) throws Exception {
        FileUtils.deleteDirectory(dir.toFile());
        lucene = new LuceneDriverWrite(dir.toString());
    }

    static void luceneClose() throws Exception {
        lucene.shutdown();
        lucene = null;
    }

    public static void process(XMLText doc) throws Exception {
        grFiller.fill(doc);
        TextInfo textInfo = KorpusFilesIterator.createTextInfo(doc);
        textInfo.subcorpus = currentSubcorpus;
        textStat.add(textInfo, doc);

        synchronized (WRITER_LOCK) {
            lucene.setTextInfo(textInfo);
            writeDoc(doc);
        }
    }

    static void writeDoc(XMLText doc) {
        List<P> ps = new ArrayList<>();
        doc.getContent().getPOrTagOrPoetry().forEach(op -> {
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
        Collections.shuffle(ps);
        ps.parallelStream().forEach(p -> {
            try {
                byte[] pa = new BinaryParagraphWriter().write(p);
                lucene.addSentence(p, pa);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
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
        public synchronized void reportError(String error) {
            Integer count = errorsCount.get(error);
            if (count == null) {
                count = 1;
            } else {
                count++;
            }
            errorsCount.put(error, count);
            System.err.println(error);
        }
    };
}
