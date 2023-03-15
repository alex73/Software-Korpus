package org.alex73.korpus.text.structure.corpus;

import java.io.Serializable;

/**
 * Class for store sentence inside binary paragraph in corpus.
 */
@SuppressWarnings("serial")
public class Sentence implements Serializable {
    public Word[] words;
}
