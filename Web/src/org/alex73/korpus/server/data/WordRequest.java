package org.alex73.korpus.server.data;

import java.io.Serializable;

@SuppressWarnings("serial")
public class WordRequest implements Serializable {
    public String word;
    public boolean allForms;
    public String grammar;
    public transient String[] lemmas; // list of calculated lemmas for find in Lucene
    public transient String[] lemmaMarks; // list of lemmas with '_' around for find in text XML

    public boolean isWildcardWord() {
        return word.indexOf('*') >= 0 || word.indexOf('?') >= 0;
    }

    @Override
    public String toString() {
        return "word=" + word + "/allForms=" + allForms + "/grammar=" + grammar;
    }
}
