package org.alex73.korpus.base;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.text.structure.corpus.Word;
import org.alex73.korpus.text.structure.files.WordItem;
import org.alex73.korpus.utils.SetUtils;

public class StaticGrammarFiller2 {
    public static boolean fillParadigmOnly;
    public static String fillTagPrefix;
    public static String fillTheme;

    private final GrammarFinder finder;
    private final Map<String, WordInfo> cache = new HashMap<>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final Lock readLock = readWriteLock.readLock();

    private final Lock writeLock = readWriteLock.writeLock();

    public StaticGrammarFiller2(GrammarFinder finder) {
        this.finder = finder;
    }

    public void fill(List<Paragraph> content) {
        content.parallelStream().forEach(p -> {
            for (Sentence se : p.sentences) {
                for (Word w : se.words) {
                    fill(w);
                }
            }
        });
    }

    private void fill(Word w) {
        WordInfo wi;
        if (w.lemmas == null && w.tags == null) {
            wi = get(w.normalized);
            if (wi == null) {
                wi = calculateWordInfo(w.normalized, null, null);
                set(w.normalized, wi);
            }
        } else {
            wi = calculateWordInfo(w.normalized, w.lemmas, w.tags);
        }
        w.lemmas = wi.lemmas;
        w.tags = wi.tags;
    }

    public void fill(WordItem w) {
        WordInfo wi;
        String expected = w.manualNormalized != null ? w.manualNormalized : w.lightNormalized;
        if (w.manualLemma == null && w.manualTag == null) {
            wi = get(expected);
            if (wi == null) {
                wi = calculateWordInfo(expected, null, null);
                set(expected, wi);
            }
        } else {
            wi = calculateWordInfo(expected, w.manualLemma, w.manualTag);
        }
        w.lemmas = wi.lemmas;
        w.tags = wi.tags;
    }

    private WordInfo get(String word) {
        readLock.lock();
        try {
            return cache.get(word);
        } finally {
            readLock.unlock();
        }
    }

    private void set(String word, WordInfo info) {
        writeLock.lock();
        try {
            cache.put(word, info);
        } finally {
            writeLock.unlock();
        }
    }

    private WordInfo calculateWordInfo(String word, String manualLemma, String manualTag) {
        StringBuilder lemmas = new StringBuilder();
        StringBuilder dbTags = new StringBuilder();
        for (Paradigm p : finder.getParadigms(word)) {
            fillTagLemmas(word, manualLemma, manualTag, lemmas, dbTags, p);
        }
        WordInfo result = new WordInfo();
        result.tags = dbTags.length() > 0 ? dbTags.toString() : null;
        result.lemmas = lemmas.length() > 0 ? lemmas.toString() : null;
        return result;
    }

    public static void fillTagLemmas(String word, String manualLemma, String manualTag, StringBuilder lemmas, StringBuilder dbTags, Paradigm p) {
        if (fillTheme != null) {
            if (!fillTheme.equals(p.getTheme())) {
                return;
            }
        }
        for (Variant v : p.getVariant()) {
            for (Form f : v.getForm()) {
                if (fillParadigmOnly) {
                    // check only first form
                    if (f != v.getForm().get(0)) {
                        break;
                    }
                }
                if (f.getValue().isEmpty()) {
                    continue;
                }
                if (manualLemma != null && !p.getLemma().equals(manualLemma)) {
                    continue;
                }
                if (BelarusianWordNormalizer.equals(f.getValue(), word)) {
                    String fTag = SetUtils.tag(p, v, f);
                    if (fillTagPrefix != null) {
                        if (!fTag.startsWith(fillTagPrefix)) {
                            continue;
                        }
                    }
                    if (manualTag != null && !manualTag.equals(fTag)) {
                        continue;
                    }
                    add(lemmas, p.getLemma());
                    add(dbTags, fTag);
                }
            }
        }
    }

    protected static void add(StringBuilder str, String value) {
        if (!SetUtils.inSeparatedList(str, value)) {
            if (str.length() > 0) {
                str.append(';');
            }
            str.append(value);
        }
    }

    static class WordInfo {
        String tags, lemmas;
    }
}
