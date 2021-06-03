package org.alex73.korpus.text.structure.files;

import org.alex73.korpus.text.structure.corpus.Word.OtherType;

public class WordItem implements ITextLineElement {
    public String lightNormalized;
    public String lemmas;
    public String tags;
    public String manualLemma;
    public String manualTag;
    public OtherType type;

    public WordItem() {
    }

    public WordItem(String text) {
        lightNormalized = text;
    }

    @Override
    public String getText() {
        return lightNormalized;
    }
}
