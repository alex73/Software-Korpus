package org.alex73.korpus.base;

import org.alex73.corpus.paradigm.Paradigm;

public interface IGrammarFinder {
    Paradigm[] getParadigmsLikeForm(String word);
}
