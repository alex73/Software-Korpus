package org.alex73.korpus.compiler;

import java.util.Properties;

public class StatInfo {
    private final String suffix;
    public int texts, words;

    public StatInfo(String suffix) {
        this.suffix = suffix.isEmpty() ? suffix : "." + suffix;
    }

    public void addText(int wordsCount) {
        texts++;
        words += wordsCount;
    }

    public void write(Properties props) {
        props.setProperty("texts" + suffix, "" + texts);
        props.setProperty("words" + suffix, "" + words);
    }
}
