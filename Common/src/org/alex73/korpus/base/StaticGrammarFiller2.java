package org.alex73.korpus.base;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
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
    private final Map<String, WordInfo> cache = new ConcurrentHashMap<>(1024 * 1024, 0.75f, 4);
    private static final ILanguage.INormalizer wordNormalizer = LanguageFactory.get("bel").getNormalizer();

    public StaticGrammarFiller2(GrammarFinder finder) {
        this.finder = finder;
    }

    public void fill(List<Paragraph> content) {
        content.stream().forEach(p -> {
            if (!"bel".equals(p.lang)) {
                return;
            }
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
            wi = cache.get(w.normalized);
            if (wi == null) {
                wi = calculateWordInfo(w.normalized, null, null);
                cache.put(w.normalized, wi);
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
            wi = cache.get(expected);
            if (wi == null) {
                wi = calculateWordInfo(expected, null, null);
                cache.put(expected, wi);
            }
        } else {
            wi = calculateWordInfo(expected, w.manualLemma, w.manualTag);
        }
        w.lemmas = wi.lemmas;
        w.tags = wi.tags;
        w.variantIds = wi.variantIds;
    }

    private WordInfo calculateWordInfo(String word, String manualLemma, String manualTag) {
        StringBuilder lemmas = new StringBuilder();
        StringBuilder dbTags = new StringBuilder();
        StringBuilder variantIds = new StringBuilder();
        for (Paradigm p : finder.getParadigms(word)) {
            fillTagLemmas(word, manualLemma, manualTag, lemmas, variantIds, dbTags, p);
        }
        WordInfo result = new WordInfo();
        result.tags = dbTags.length() > 0 ? dbTags.toString() : null;
        result.lemmas = lemmas.length() > 0 ? lemmas.toString() : null;
        result.variantIds = variantIds.length() > 0 ? variantIds.toString() : null;
        return result;
    }

    public static void fillTagLemmas(String word, String manualLemma, String manualTag, StringBuilder lemmas, StringBuilder variantIds, StringBuilder dbTags, Paradigm p) {
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
                if (wordNormalizer.equals(f.getValue(), word)) {
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
                    add(variantIds, p.getPdgId() + v.getId());
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
        String tags, lemmas, variantIds;
    }
}
