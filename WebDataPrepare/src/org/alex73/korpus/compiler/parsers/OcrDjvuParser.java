package org.alex73.korpus.compiler.parsers;

import java.nio.file.Path;

import org.alex73.korpus.compiler.BaseParallelProcessor;

public class OcrDjvuParser extends BaseParser {
    public OcrDjvuParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(BaseParallelProcessor queue, boolean headersOnly) throws Exception {
    }
}
