package org.alex73.korpus.compiler.parsers;

import java.nio.file.Path;
import java.util.Arrays;

import org.alex73.korpus.compiler.BaseParallelProcessor;
import org.alex73.korpus.utils.KorpusDateTime;

public abstract class BaseParser implements IParser {
    static final int BUFFER = 256 * 1024;

    protected final String subcorpus;
    protected final Path file;

    public BaseParser(String subcorpus, Path file) {
        this.subcorpus = subcorpus;
        this.file = file;
    }

    public abstract void parse(BaseParallelProcessor queue, boolean headersOnly) throws Exception;

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
}
