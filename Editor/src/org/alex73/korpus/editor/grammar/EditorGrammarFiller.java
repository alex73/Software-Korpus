package org.alex73.korpus.editor.grammar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.text.structure.files.ITextLineElement;
import org.alex73.korpus.text.structure.files.TextLine;
import org.alex73.korpus.text.structure.files.WordItem;
import org.alex73.korpus.utils.SetUtils;

public class EditorGrammarFiller {
    protected final GrammarDB2 db;
    private final StaticGrammarFiller2 staticFiller;
    private final List<Paradigm> newParadigms;

    public EditorGrammarFiller(GrammarDB2 db, StaticGrammarFiller2 staticFiller, List<Paradigm> newParadigms) {
        this.db = db;
        this.staticFiller = staticFiller;
        this.newParadigms = newParadigms;
    }

    /**
     * Returns list of paradigms that can be used for specific word.
     */
    @Deprecated
    public List<Paradigm> getParadigms(String w) {
        List<Paradigm> result=Collections.synchronizedList(new ArrayList<>());
        
        String word = BelarusianWordNormalizer.lightNormalized(w);

        db.getAllParadigms().parallelStream().forEach(p -> {
            if (checkParadigm(word, p)) {
                result.add(p);
            }
        });
        newParadigms.parallelStream().forEach(p -> {
            if (checkParadigm(word, p)) {
                result.add(p);
            }
        });
        return result;
    }

    /**
     * Fills grammar for all words except manually choosed previously.
     */
    public void fill(List<TextLine> lines) {
        lines.parallelStream().forEach(line -> {
            for (ITextLineElement it : line) {
                if (it instanceof WordItem) {
                    fill((WordItem) it);
                }
            }
        });
    }

    public void fill(WordItem wi) {
        staticFiller.fill(wi);

        String expected = wi.manualNormalized != null ? wi.manualNormalized : wi.lightNormalized;
        StringBuilder lemmas = new StringBuilder();
        StringBuilder dbTags = new StringBuilder();
        newParadigms.stream().forEach(p -> StaticGrammarFiller2.fillTagLemmas(expected, wi.manualLemma, wi.manualTag, lemmas, dbTags, p));
        wi.tags = addIfNeed(wi.tags, dbTags.toString());
        wi.lemmas = addIfNeed(wi.lemmas, lemmas.toString());
    }

    @Deprecated
    private boolean checkParadigm(String word, Paradigm p) {
        for(Variant v:p.getVariant()) {
            for(Form f:v.getForm()) {
                if (f.getValue() == null || f.getValue().isEmpty()) {
                    continue;
                }
                if (BelarusianWordNormalizer.equals(f.getValue(), word)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Дадаем толькі новыя тэгі і лемы, бо асноўныя знойдзены праз staticFiller.
     */
    private String addIfNeed(String prevList, String newPart) {
        if (newPart.isEmpty()) {
            return prevList;
        }
        if (prevList == null) {
            return newPart;
        }

        StringBuilder add = new StringBuilder();
        for (String v : newPart.split(";")) {
            if (!SetUtils.inSeparatedList(prevList, v)) {
                add.append(';').append(v);
            }
        }
        return prevList + add;
    }
}
