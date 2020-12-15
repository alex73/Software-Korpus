package org.alex73.korpus.compiler.parsers;

import java.util.concurrent.Executor;

public interface IParser {
    void parse(Executor queue) throws Exception;
}
