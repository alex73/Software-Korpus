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
import org.alex73.korpus.text.elements.Paragraph;
import org.alex73.korpus.text.elements.Sentence;
import org.alex73.korpus.text.elements.Word;
import org.alex73.korpus.utils.SetUtils;

public class StaticGrammarFiller2 {
    private final GrammarFinder finder;
    private final Map<String, WordInfo> cache = new HashMap<>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final Lock readLock = readWriteLock.readLock();

    private final Lock writeLock = readWriteLock.writeLock();

    public StaticGrammarFiller2(GrammarFinder finder) {
        this.finder = finder;
    }

    public void fill(List<Paragraph> content) {
        for (Paragraph p : content) {
            for (Sentence se : p.sentences) {
                for (Word w : se.words) {
                    WordInfo wi = get(w.lightNormalized);
                    if (wi == null) {
                        wi = calculateWordInfo(w.lightNormalized);
                        set(w.lightNormalized, wi);
                    }
                    w.lemmas = wi.lemmas;
                    w.tags = wi.tags;
                }
            }
        }
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

    private WordInfo calculateWordInfo(String word) {
        StringBuilder lemmas = new StringBuilder();
        StringBuilder dbTags = new StringBuilder();
        for (Paradigm p : finder.getParadigms(word)) {
            for (Variant v : p.getVariant()) {
                for (Form f : v.getForm()) {
                    if (f.getValue().isEmpty()) {
                        continue;
                    }
                    if (BelarusianWordNormalizer.equals(f.getValue(), word)) {
                        add(lemmas, p.getLemma());
                        add(dbTags, SetUtils.tag(p, v, f));
                    }
                }
            }
        }
        WordInfo result = new WordInfo();
        result.tags = dbTags.length() > 0 ? dbTags.toString() : null;
        result.lemmas = lemmas.length() > 0 ? lemmas.toString() : null;
        return result;
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
