package org.alex73.korpus.compiler.parsers;

import java.nio.file.Path;
import java.util.concurrent.Executor;

public class OcrDjvuParser extends BaseParser {
    public OcrDjvuParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(Executor queue) throws Exception {
        System.out.println(file);
    }
}
