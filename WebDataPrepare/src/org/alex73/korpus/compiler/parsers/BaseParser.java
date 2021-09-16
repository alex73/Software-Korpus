package org.alex73.korpus.compiler.parsers;

import java.nio.file.Path;
import java.util.Arrays;

import org.alex73.korpus.compiler.BaseParallelProcessor;

public abstract class BaseParser implements IParser {
    static final int BUFFER = 256 * 1024;

    protected final String subcorpus;
    protected final Path file;

    public BaseParser(String subcorpus, Path file) {
        this.subcorpus = subcorpus;
        this.file = file;
    }

    public abstract void parse(BaseParallelProcessor queue, boolean headersOnly) throws Exception;

    protected String[] trims(String[] list) {
        for (int i = 0; i < list.length; i++) {
            list[i] = list[i].trim();
            if (list[i].isEmpty()) {
                throw new RuntimeException("Wrong list data: " + Arrays.toString(list));
            }
        }
        return list;
    }
}
