package org.alex73.korpus.base;

import org.alex73.grammardb.structures.Paradigm;

public interface IGrammarFinder {
    Paradigm[] getParadigms(String word);
}
