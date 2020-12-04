package org.alex73.korpus.compiler.parsers;

import java.nio.file.Path;

public interface IParser {
    static final int BUFFER = 256 * 1024;

    void parse(Path file) throws Exception;
}
