package org.alex73.korpus.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.zip.ZipOutputStream;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.stat.StatWriting;
import org.alex73.korpus.compiler.stat.WordsStat;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.text.structure.corpus.Word;

public class ProcessStat extends BaseParallelProcessor<MessageParsedText> {

    private static final Pattern RE_SPLIT = Pattern.compile(";");

    private final Path tempOutputDir;
    // stat by subcorpuses
    private final Map<String, TextStatInfo> textStatInfos = new HashMap<>();
    // authors by lemmas: Map<lemma, Set<author>>
    private final Map<String, Set<String>> authorsByLemmas = new HashMap<>();
    private final Map<String, WordsStat> wordsStatsBySubcorpus = new HashMap<>();

    public ProcessStat(boolean processStat, Path tempOutputDir) throws Exception {
        super(8, 16);
        this.tempOutputDir = tempOutputDir;
    }

    @Override
    public void accept(MessageParsedText text) {
        run(() -> {
            for (int i = 0; i < text.textInfo.subtexts.length; i++) {
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
        });
    }

    WordsStat calcWordsStat(MessageParsedText text, int textIndex) {
        WordsStat wordsStat = new WordsStat(null);
        for (Paragraph[] p : text.paragraphs) {
            for (Sentence se : p[textIndex].sentences) {
                for (Word w : se.words) {
                    String[] lemmas = w.wordNormalized == null ? null : RE_SPLIT.split(w.wordNormalized);
                    wordsStat.addWord(w.wordNormalized, lemmas);
                }
            }
        }
        return wordsStat;
    }

    void addGlobalCounts(TextInfo textInfo, int textIndex, int wordsCount) {
        TextStatInfo subcorpusStatInfo = getTextStatInfo(textInfo.subcorpus);
        subcorpusStatInfo.texts.incrementAndGet();
        subcorpusStatInfo.words.addAndGet(wordsCount);
        if (textInfo.subtexts[textIndex].authors != null) {
            for (String a : textInfo.subtexts[textIndex].authors) {
                subcorpusStatInfo.authors.add(a);
            }
        }
        if (textInfo.subtexts[textIndex].source != null) {
            subcorpusStatInfo.sources.add(textInfo.subtexts[textIndex].source);
        }
        switch (textInfo.subcorpus) {
        case "teksty":
            if (textInfo.styleGenres != null) {
                Arrays.asList(textInfo.styleGenres).stream().map(s -> s.replaceAll("/.+", "")).sorted().distinct().forEach(s -> {
                    TextStatInfo subcorpusStyleGenre = getTextStatInfo(textInfo.subcorpus + "." + s);
                    subcorpusStyleGenre.texts.incrementAndGet();
                    subcorpusStyleGenre.words.addAndGet(wordsCount);
                });
            }
            break;
        case "sajty":
        case "nierazabranaje":
            TextStatInfo subcorpusSource = getTextStatInfo(textInfo.subcorpus + "." + textInfo.subtexts[textIndex].source);
            subcorpusSource.texts.incrementAndGet();
            subcorpusSource.words.addAndGet(wordsCount);
            break;
        }
    }

    public void finish(Path outputDir) throws Exception {
        super.finish(10);
        for (WordsStat ws : wordsStatsBySubcorpus.values()) {
            ws.closeFile();
        }
        StatWriting.write(textStatInfos, authorsByLemmas, outputDir);
    }

    public void mergeToZip(ZipOutputStream zip, Consumer<ExRunnable> processor) throws Exception {
        List<Path> allCounts = wordsStatsBySubcorpus.keySet().stream().map(subcorpus -> getTempFreqFile(subcorpus)).toList();
        processor.accept(() -> StatWriting.mergeCounts(allCounts, zip, "forms/freq.tab"));

        for (String subcorpus : wordsStatsBySubcorpus.keySet()) {
            processor.accept(() -> StatWriting.mergeCounts(List.of(getTempFreqFile(subcorpus)), zip, "forms/freq." + subcorpus + ".tab"));
        }
    }

    public void removeTemp() throws IOException {
        for (String subcorpus : wordsStatsBySubcorpus.keySet()) {
            Files.delete(getTempFreqFile(subcorpus));
        }
    }

    private Path getTempFreqFile(String subcorpus) {
        return tempOutputDir.resolve("temp-stat-" + subcorpus + ".snappy");
    }

    private void addAuthorByLemma(String lemma, String author) {
        Set<String> authors;
        synchronized (authorsByLemmas) {
            authors = authorsByLemmas.computeIfAbsent(lemma, l -> new HashSet<>());
        }
        synchronized (authors) {
            authors.add(author);
        }
    }

    private TextStatInfo getTextStatInfo(String key) {
        synchronized (textStatInfos) {
            return textStatInfos.computeIfAbsent(key, k -> new TextStatInfo());
        }
    }

    private WordsStat getWordsStatBySubcorpus(String key) {
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
