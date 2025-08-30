package org.alex73.korpus.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipOutputStream;

import org.alex73.grammardb.GrammarFinder;
import org.alex73.grammardb.StressUtils;
import org.alex73.grammardb.structures.Form;
import org.alex73.grammardb.structures.Paradigm;
import org.alex73.grammardb.structures.Variant;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.stat.StatWriting;
import org.alex73.korpus.compiler.stat.WordsStat;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.text.structure.corpus.Word;

public class Step3Stat {
    public static final int MAX_STAT_IN_MEMORY = 100000;

    private static final String LANG = "bel";

    private static GrammarFinder grFinder;
    private static Path tempOutputDir;
    // stat by subcorpuses
    private static Map<String, TextStatInfo> textStatInfos = new HashMap<>();
    // authors by lemmas: Map<lemma, Set<author>>
    private static Map<String, Set<String>> authorsByLemmas = new HashMap<>();
    public static Map<String, WordsStat> wordsStatsBySubcorpus = new HashMap<>();

    public static void init(Path tempDir, GrammarFinder finder) throws Exception {
        tempOutputDir = tempDir;
        grFinder = finder;
    }

    public static void run(MessageParsedText text) throws Exception {
        for (int i = 0; i < text.textInfo.subtexts.length; i++) {
            if (!LANG.equals(text.languages[i].lang)) {
                continue;
            }
            WordsStat stat = calcWordsStat(text, i);
            getWordsStatBySubcorpus(text.textInfo.subcorpus).mergeFrom(stat);
            addGlobalCounts(text.textInfo, i, stat.getWordsCount());
            if (text.textInfo.subtexts[i].authors != null) {
                for (String author : text.textInfo.subtexts[i].authors) {
                    for (String lemma : stat.getAllLemmas()) {
                        addAuthorByLemma(lemma, author);
                    }
                }
            }
        }
    }

    private static WordsStat calcWordsStat(MessageParsedText text, int textIndex) {
        WordsStat wordsStat = new WordsStat(null);
        for (Paragraph p : text.languages[textIndex].paragraphs) {
            for (Sentence se : p.sentences) {
                for (Word w : se.words) {
                    if (w.wordNormalized != null) {
                        // can be null in case of tail or service mark
                        wordsStat.addWord(w.wordNormalized, getLemmasByForm(w.wordNormalized));
                    }
                }
            }
        }
        return wordsStat;
    }

    private static Set<String> getLemmasByForm(String form) {
        Set<String> result = new TreeSet<>();
        for (Paradigm p : grFinder.getParadigms(form)) {
            for (Variant v : p.getVariant()) {
                for (Form f : v.getForm()) {
                    if (fastCompareWithoutStress(f.getValue(), form)) {
                        result.add(p.getLemma());
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static boolean fastCompareWithoutStress(String s1, String s2) {
        int p1 = 0, p2 = 0;
        while (true) {
            char c1 = p1 < s1.length() ? s1.charAt(p1) : 0;
            char c2 = p2 < s2.length() ? s2.charAt(p2) : 0;
            if (c1 == 0 && c2 == 0) {
                return true;
            }
            if (StressUtils.STRESS_CHARS.indexOf(c1) >= 0) {
                p1++;
                continue;
            }
            if (StressUtils.STRESS_CHARS.indexOf(c2) >= 0) {
                p2++;
                continue;
            }
            if (Character.isUpperCase(c1)) {
                c1 = Character.toLowerCase(c1);
            }
            if (Character.isUpperCase(c2)) {
                c2 = Character.toLowerCase(c2);
            }
            if (c1 != c2) {
                return false;
            }
            p1++;
            p2++;
        }
    }

    private static void addGlobalCounts(TextInfo textInfo, int textIndex, int wordsCount) {
        TextStatInfo commonStatInfo = getTextStatInfo("");
        commonStatInfo.texts.incrementAndGet();
        commonStatInfo.words.addAndGet(wordsCount);
        TextStatInfo subcorpusStatInfo = getTextStatInfo(textInfo.subcorpus);
        subcorpusStatInfo.texts.incrementAndGet();
        subcorpusStatInfo.words.addAndGet(wordsCount);
        if (textInfo.subtexts[textIndex].authors != null) {
            for (String a : textInfo.subtexts[textIndex].authors) {
                subcorpusStatInfo.authors.add(a);
            }
        }
        if (textInfo.subtexts[textIndex].sourceName != null) {
            subcorpusStatInfo.sources.add(textInfo.subtexts[textIndex].sourceName);
        }
        if (textInfo.styleGenres != null) {
            Arrays.asList(textInfo.styleGenres).stream().map(s -> s.replaceAll("/.+", "")).sorted().distinct().forEach(s -> {
                TextStatInfo subcorpusStyleGenre = getTextStatInfo(textInfo.subcorpus + "." + s);
                subcorpusStyleGenre.texts.incrementAndGet();
                subcorpusStyleGenre.words.addAndGet(wordsCount);
            });
        }
        if (textInfo.subtexts[textIndex].sourceName != null) {
            TextStatInfo subcorpusSource = getTextStatInfo(textInfo.subcorpus + "." + textInfo.subtexts[textIndex].sourceName);
            subcorpusSource.texts.incrementAndGet();
            subcorpusSource.words.addAndGet(wordsCount);
        }
    }

    public static void flush() throws Exception {
        for (WordsStat ws : wordsStatsBySubcorpus.values()) {
            ws.flush();
        }
    }

    public static void finish(Path outputDir) throws Exception {
        for (WordsStat ws : wordsStatsBySubcorpus.values()) {
            ws.closeFile();
        }
        StatWriting.write(textStatInfos, authorsByLemmas, outputDir);
    }

    public static void mergeToZip(ZipOutputStream zip) throws Exception {
        List<Runnable> runs = new ArrayList<>();

        List<Path> allCounts = wordsStatsBySubcorpus.keySet().stream().map(subcorpus -> getTempFreqFile(subcorpus)).toList();
        runs.add(() -> StatWriting.mergeCounts(allCounts, zip, "forms/freq.tab"));

        for (String subcorpus : wordsStatsBySubcorpus.keySet()) {
            runs.add(() -> StatWriting.mergeCounts(List.of(getTempFreqFile(subcorpus)), zip, "forms/freq." + subcorpus + ".tab"));
        }

        runs.parallelStream().forEach(r -> r.run());
    }

    public static void removeTemp() throws IOException {
        for (String subcorpus : wordsStatsBySubcorpus.keySet()) {
            Files.delete(getTempFreqFile(subcorpus));
        }
    }

    private static Path getTempFreqFile(String subcorpus) {
        return tempOutputDir.resolve("temp-stat-" + subcorpus + ".snappy");
    }

    private static void addAuthorByLemma(String lemma, String author) {
        Set<String> authors;
        synchronized (authorsByLemmas) {
            authors = authorsByLemmas.computeIfAbsent(lemma, l -> new HashSet<>());
        }
        synchronized (authors) {
            authors.add(author);
        }
    }

    private static TextStatInfo getTextStatInfo(String key) {
        synchronized (textStatInfos) {
            return textStatInfos.computeIfAbsent(key, k -> new TextStatInfo());
        }
    }

    private static WordsStat getWordsStatBySubcorpus(String key) {
        synchronized (wordsStatsBySubcorpus) {
            return wordsStatsBySubcorpus.computeIfAbsent(key, k -> new WordsStat(getTempFreqFile(k)));
        }
    }

    public static class TextStatInfo {
        public AtomicLong texts = new AtomicLong();
        public AtomicLong words = new AtomicLong();
        public Set<String> authors = Collections.synchronizedSet(new HashSet<>());
        public Set<String> sources = Collections.synchronizedSet(new HashSet<>());
    }
}
