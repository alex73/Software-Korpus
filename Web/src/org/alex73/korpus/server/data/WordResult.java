package org.alex73.korpus.server.data;

import org.alex73.korpus.text.structure.corpus.Word;

@SuppressWarnings("serial")
public class WordResult extends Word {
    /** True if word is requested by user, i.e. should be marked in output. */
    public Boolean requestedWord;

    public WordResult(Word w) {
        this.lightNormalized = w.lightNormalized;
        this.lemmas = w.lemmas;
        this.tags = w.tags;
        this.tail = w.tail;
    }

    @Override
    public String toString() {
        return lightNormalized + tail;
    }
}
