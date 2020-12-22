package org.alex73.korpus.compiler.parsers;

import java.nio.file.Path;
import java.util.concurrent.Executor;

public abstract class BaseParser implements IParser {
    static final int BUFFER = 256 * 1024;

    protected final String subcorpus;
    protected final Path file;

    public BaseParser(String subcorpus, Path file) {
        this.subcorpus = subcorpus;
        this.file = file;
    }

    public abstract void parse(Executor queue, boolean headersOnly) throws Exception;
}
