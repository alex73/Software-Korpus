package org.alex73.korpus.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarFiller;
import org.alex73.korpus.base.GrammarFinder;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.server.engine.LuceneDriverWrite;
import org.alex73.korpus.server.text.BinaryParagraphWriter;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.XMLText;
import org.apache.commons.io.FileUtils;

public class PrepareCache2 {
    static GrammarFiller grFiller;
    static LuceneDriverWrite lucene;

    static Set<String> korpusAuthors = new TreeSet<>();
    static Properties korpusStat = new Properties();
    static Map<String, StatInfo> korpusParts = new HashMap<>();
    static StatInfo korpusTotal = new StatInfo("");

    static Properties otherStat = new Properties();
    static StatInfo otherTotal = new StatInfo("");
    static Set<String> otherVolumes = new TreeSet<>();

    public static void main(String[] args) throws Exception {
        System.out.println("Load GrammarDB...");
        GrammarDB2 gr = GrammarDB2.initializeFromDir("GrammarDB");
        grFiller = new GrammarFiller(new GrammarFinder(gr));

        final ExecutorService readers = Executors.newFixedThreadPool(1);
        final BlockingQueue<XMLText> readers2preprocessors = new ArrayBlockingQueue<>(1);
        final ExecutorService preprocessors = Executors.newFixedThreadPool(1);
        final BlockingQueue<XMLText> preprocessors2writers = new ArrayBlockingQueue<>(1);

        // main texts corpus
        luceneOpen("Korpus-cache/");
        System.out.println("rstart");
        new KorpusFilesIterator(errors, readers2preprocessors, "Korpus-texts/").process(readers);
        System.out.println("rstop");

        AtomicInteger work = new AtomicInteger(8);
        for (int i = 0; i < 8; i++) {
            preprocessors.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            XMLText doc = readers2preprocessors.take();
                            if (doc == null) {
                                readers2preprocessors.add(null);
                                work.decrementAndGet();
                                break;
                            }
                            System.out.println("fill "+readers2preprocessors.size());
                            grFiller.fill(doc);
                            preprocessors2writers.put(doc);
                        }
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
        }

        while (true) {
            XMLText doc = preprocessors2writers.poll(1, TimeUnit.SECONDS);
            if (doc == null && work.get() == 0) {
                break;
            }
            System.out.println("save "+preprocessors2writers.size());
            TextInfo textInfo = KorpusFilesIterator.createTextInfo(doc);
            lucene.setTextInfo(textInfo);

            for (String a : textInfo.authors) {
                korpusAuthors.add(a);
            }

            int wc = wordsCount(doc);
            korpusTotal.addText(wc);
            if (textInfo.styleGenres.length > 0) {
                for (String s : textInfo.styleGenres) {
                    addKorpusStat(s, wc);
                }
            } else {
                addKorpusStat("_", wc);
            }

            writeDoc(doc);
        }

        luceneClose();

        for (StatInfo si : korpusParts.values()) {
            si.write(korpusStat); // - don't output details yet
        }
        korpusTotal.write(korpusStat);
        String authorsstr = "";
        for (String s : korpusAuthors) {
            authorsstr += ";" + s;
        }
        if (!korpusAuthors.isEmpty()) {
            korpusStat.setProperty("authors", authorsstr.substring(1));
        } else {
            korpusStat.setProperty("authors", "");
        }
        writeStat("Korpus-cache/", korpusStat);
if (true) return;
        // other trash corpus
        luceneOpen("Other-cache/");
        new OtherFilesIterator(errors, processOtherKorpus).iterate("Other-texts/");
        luceneClose();
        otherTotal.write(otherStat);
        String volumesstr = "";
        for (String s : otherVolumes) {
            volumesstr += ";" + s;
        }
        if (!volumesstr.isEmpty()) {
            otherStat.setProperty("volumes", volumesstr.substring(1));
        } else {
            otherStat.setProperty("volumes", "");
        }
        writeStat("Other-cache/", otherStat);

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
    }

    static void luceneOpen(String dir) throws Exception {
        File d = new File(dir);
        FileUtils.deleteDirectory(d);
        d.mkdirs();
        lucene = new LuceneDriverWrite(dir);
    }

    static void luceneClose() throws Exception {
        lucene.shutdown();
        lucene = null;
    }

    static IFilesIterator processOtherKorpus = new IFilesIterator() {
        @Override
        public void onText(XMLText doc) throws Exception {
            grFiller.fill(doc);

            otherVolumes.add("kamunikat.org");
            String id = OtherFilesIterator.getId(doc);
            lucene.setOtherInfo("kamunikat.org", "http://kamunikat.org/halounaja.html?pubid=" + id);

            int wc = wordsCount(doc);
            otherTotal.addText(wc);

            writeDoc(doc);
        }
    };

    static void writeDoc(XMLText doc) {
        List<P> ps = new ArrayList<>();
        doc.getContent().getPOrTagOrPoetry().forEach(op -> {
            if (op instanceof P) {
                ps.add((P) op);
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

    static int wordsCount(XMLText doc) {
        AtomicInteger r = new AtomicInteger();
        doc.getContent().getPOrTagOrPoetry().parallelStream().forEach(op -> {
            if (op instanceof P) {
                ((P) op).getSe().forEach(s -> {
                    s.getWOrSOrZ().forEach(ow -> {
                        if (ow instanceof W) {
                            r.incrementAndGet();
                        }
                    });
                });
            }
        });
        return r.get();
    }

    static void writeStat(String dir, Properties stat) throws Exception {
        FileOutputStream o = new FileOutputStream(dir + "/stat.properties");
        stat.store(o, null);
        o.close();
    }

    static void addKorpusStat(String styleGenre, int wordsCount) {
        int p = styleGenre.indexOf('/');
        String st = p < 0 ? styleGenre : styleGenre.substring(0, p);
        StatInfo i = korpusParts.get(st);
        if (i == null) {
            i = new StatInfo(st);
            korpusParts.put(st, i);
        }
        i.addText(wordsCount);
    }

    private static Map<String, Integer> errorsCount = new HashMap<>();
    static IProcess errors = new IProcess() {
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
