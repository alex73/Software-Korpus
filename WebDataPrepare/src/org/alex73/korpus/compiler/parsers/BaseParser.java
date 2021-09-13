package org.alex73.korpus.compiler.parsers;

import java.nio.file.Path;

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
}
