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
import org.alex73.korpus.editor.core.doc.structure.WordItem;
import org.alex73.korpus.text.elements.Paragraph;
import org.alex73.korpus.text.elements.Sentence;
import org.alex73.korpus.text.elements.Word;
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
    public void fillNonManual(List<Paragraph> paragraphs) {
        staticFiller.fillNonManual(paragraphs);
        paragraphs.parallelStream().forEach(par->{
            for (Sentence se : par.sentences) {
                for (Word w : se.words) {
                    if (w.manualGrammar) {
                        continue;
                    }
                    newParadigms.stream().forEach(p -> fillFromParadigm(w, p));
                }
            }
        });
    }

    public void fillNonManual(WordItem w) {
        if (w.manualGrammar) {
            return;
        }
        Word t = new Word();
        t.lightNormalized = w.lightNormalized;
        staticFiller.fillNonManual(t);
        newParadigms.stream().forEach(p -> fillFromParadigm(t, p));
        w.lemmas = t.lemmas;
        w.tags = t.tags;
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
    public void fillFromParadigm(Word w, Paradigm p) {
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
