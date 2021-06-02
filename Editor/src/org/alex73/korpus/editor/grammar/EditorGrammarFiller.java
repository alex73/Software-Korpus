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
    public void fillNonManual(List<TextLine> lines) {
        lines.parallelStream().forEach(line -> {
            for (ITextLineElement it : line) {
                if (it instanceof WordItem) {
                    fillNonManual((WordItem) it);
                }
            }
        });
    }

    public void fillNonManual(WordItem wi) {
        if (wi.manualGrammar || wi.type != null) {
            return;
        }
        staticFiller.fill(wi);
        newParadigms.stream().forEach(p -> fillFromParadigm(wi, p));
    }

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
     * Add grammar from specific paradigm if need.
     */
    public void fillFromParadigm(WordItem w, Paradigm p) {
        p.getVariant().forEach(v -> {
            v.getForm().forEach(f -> {
                if (f.getValue() == null || f.getValue().isEmpty()) {
                    return;
                }
                if (BelarusianWordNormalizer.equals(f.getValue(), w.lightNormalized)) {
                    w.lemmas = addIfNeed(w.lemmas, p.getLemma());
                    w.tags = addIfNeed(w.tags, SetUtils.tag(p, v, f));
                }
            });
        });
    }

    /**
     * Add part of name to string.
     */
    private String addIfNeed(String prevList, String newPart) {
        if (prevList == null) {
            return newPart;
        }
        String[] prev = prevList.split(";");
        for (String s : prev) {
            if (s.equals(newPart)) {
                return prevList;
            }
        }
        return prevList + ';' + newPart;
    }
}
