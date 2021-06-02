package org.alex73.korpus.text.structure.files;

import org.alex73.korpus.text.structure.corpus.Word;
import org.alex73.korpus.text.structure.corpus.Word.OtherType;

public class WordItem implements ITextLineElement {
    public String lightNormalized;
    public String lemmas;
    public String tags;
    public boolean manualGrammar;
    public OtherType type;

    public WordItem() {
    }

    public WordItem(String text) {
        lightNormalized = text;
    }

    public WordItem(Word w) {
        lightNormalized = w.lightNormalized;
        lemmas = w.lemmas;
        tags = w.tags;
        manualGrammar = w.manualGrammar;
        type = w.type;
    }

    public Word exractWord() {
        Word w = new Word();
        w.lightNormalized = lightNormalized;
        w.lemmas = lemmas;
        w.tags = tags;
        w.manualGrammar = manualGrammar;
        w.type = type;
        return w;
    }

    @Override
    public String getText() {
        return lightNormalized;
    }
}
