package org.alex73.korpus.editor.grammar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alex73.grammardb.GrammarDB2;
import org.alex73.grammardb.SetUtils;
import org.alex73.grammardb.structures.Form;
import org.alex73.grammardb.structures.Paradigm;
import org.alex73.grammardb.structures.Variant;
import org.alex73.korpus.base.GrammarFinder;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.languages.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.text.structure.files.ITextLineElement;
import org.alex73.korpus.text.structure.files.TextLine;
import org.alex73.korpus.text.structure.files.WordItem;

public class EditorGrammarFiller {
    protected final GrammarDB2 db;
    private final StaticGrammarFiller2 staticFiller;
    private final List<Paradigm> newParadigms;
    private StaticGrammarFiller2 cachedFiller; // based on newParadigms

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

    public void cacheAgain() {
        GrammarDB2 gr = GrammarDB2.empty();
        gr.getAllParadigms().addAll(newParadigms);
        GrammarFinder finder = new GrammarFinder(gr);
        cachedFiller = new StaticGrammarFiller2(finder);
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
        WordItem wi2 = wi.clone();
        cachedFiller.fill(wi2);
        wi.tags = addIfNeed(wi.tags, wi2.tags);
        wi.lemmas = addIfNeed(wi.lemmas, wi2.lemmas);
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
        if (newPart == null || newPart.isEmpty()) {
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
