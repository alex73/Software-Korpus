package org.alex73.korpus.shared.dto;

import java.io.Serializable;

public class WordResult implements Serializable {
    public String orig;
    public String normalized;
    public String cat;
    public String lemma;
    /** True if word is requested by user, i.e. should be marked in output. */
    public boolean requestedWord;

    public boolean isWord;

    @Override
    public String toString() {
        return orig;
    }
}
