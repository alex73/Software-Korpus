package org.alex73.korpus.compiler.parsers;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.utils.KorpusDateTime;

public abstract class BaseParser implements IParser {
    static final int BUFFER = 256 * 1024;

    protected final String subcorpus;
    protected final Path file;

    public BaseParser(String subcorpus, Path file) {
        this.subcorpus = subcorpus;
        this.file = file;
    }

    protected String[] splitAndTrim(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String[] list = s.split(";");
        for (int i = 0; i < list.length; i++) {
            list[i] = list[i].trim();
            if (list[i].isEmpty()) {
                throw new RuntimeException("Wrong list data: " + Arrays.toString(list));
            }
        }
        return list;
    }

    protected String getAndCheckYears(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        new KorpusDateTime(s);
        return s;
    }

    protected String getLang(String v) {
        return v == null ? "bel" : v;
    }

    protected Paragraph[][] get1LangParagraphs(List<Paragraph> ps) {
        Paragraph[][] r = new Paragraph[ps.size()][];
        for (int i = 0; i < r.length; i++) {
            r[i] = new Paragraph[1];
            r[i][0] = ps.get(i);
        }
        return r;
    }
}
