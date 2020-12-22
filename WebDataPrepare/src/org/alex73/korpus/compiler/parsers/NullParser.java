package org.alex73.korpus.compiler.parsers;

import java.nio.file.Path;
import java.util.concurrent.Executor;

public class NullParser extends BaseParser {
    public NullParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(Executor queue, boolean headersOnly) throws Exception {
    }
}
