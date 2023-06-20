package org.alex73.korpus.base;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.text.structure.corpus.Word;
import org.alex73.korpus.utils.SetUtils;

public class StaticGrammarFiller2 {
    public static boolean fillParadigmOnly;
    public static String fillTagPrefix;
    public static String fillTheme;

    private final GrammarFinder finder;
    private final Map<String, WordInfo> cache = new ConcurrentHashMap<>(1024 * 1024, 0.75f, 4);
    private static final ILanguage.INormalizer wordNormalizer = LanguageFactory.get("bel").getNormalizer();

    public StaticGrammarFiller2(GrammarFinder finder) {
        this.finder = finder;
    }

    public GrammarFinder getFinder() {
        return finder;
    }

    public void fill(Paragraph[] content, boolean fillTags) {
        for (Paragraph p : content) {
            for (Sentence se : p.sentences) {
                for (Word w : se.words) {
                    fill(w, fillTags && "bel".equals(p.lang));
                }
            }
        }
    }

    public void fill(Word w, boolean fillTags) {
        if (w.type != null || w.word == null) {
            return;
        }
        WordInfo wi;
        w.wordZnakNormalized = wordNormalizer.znakNormalized(w.word, ILanguage.INormalizer.PRESERVE_NONE);
        w.wordNormalized = wordNormalizer.lightNormalized(w.word, ILanguage.INormalizer.PRESERVE_NONE);
        w.wordSuperNormalized = wordNormalizer.superNormalized(w.word, ILanguage.INormalizer.PRESERVE_NONE);
        if (fillTags) {
            wi = cache.computeIfAbsent(w.wordNormalized, wn -> createCacheEntry(w));
            w.tagsNormalized = wi.tagsNormalized;
            w.tagsVariants = wi.tagsWriteVariant;
        }
    }

    private WordInfo createCacheEntry(Word w) {
        StringBuilder dbTagsNormalized = new StringBuilder();
        StringBuilder dbTagsWriteVariant = new StringBuilder();
        for (Paradigm p : finder.getParadigms(w.wordSuperNormalized)) {
            fillTagLemmas(w.wordNormalized, dbTagsNormalized, p, wo -> wordNormalizer.lightNormalized(wo, ILanguage.INormalizer.PRESERVE_NONE));
            fillTagLemmas(w.wordSuperNormalized, dbTagsWriteVariant, p, wo -> wordNormalizer.superNormalized(wo, ILanguage.INormalizer.PRESERVE_NONE));
        }
        WordInfo result = new WordInfo();
        result.tagsNormalized = dbTagsNormalized.length() > 0 ? dbTagsNormalized.toString().intern() : null;
        result.tagsWriteVariant = dbTagsWriteVariant.length() > 0 ? dbTagsWriteVariant.toString().intern() : null;
        return result;
    }

    public static void fillTagLemmas(String expectedWord, StringBuilder outputTags, Paradigm p, Function<String, String> normalize) {
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
                if (expectedWord.equals(normalize.apply(f.getValue()))) {
                    String fTag = SetUtils.tag(p, v, f);
                    if (fillTagPrefix != null) {
                        if (!fTag.startsWith(fillTagPrefix)) {
                            continue;
                        }
                    }
                    add(outputTags, fTag);
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
        String tagsNormalized, tagsWriteVariant;
    }
}
