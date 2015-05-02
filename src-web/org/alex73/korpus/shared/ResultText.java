package org.alex73.korpus.shared;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ResultText implements Serializable {
    public Word[][] words; // paragraph is array of sentences, i.e. of array of words

    public static class Word implements Serializable {
        public String value;
        public String cat;
        public String lemma;
        /** True if word is requested by user, i.e. should be marked in output. */
        public boolean requestedWord;

        @Override
        public String toString() {
            return value;
        }
    }
}
