package org.alex73.korpus.compiler.parsers;

import java.nio.file.Path;

public class OcrDjvuParser implements IParser {
    @Override
    public void parse(Path file) throws Exception {
        System.out.println(file);
    }
}
