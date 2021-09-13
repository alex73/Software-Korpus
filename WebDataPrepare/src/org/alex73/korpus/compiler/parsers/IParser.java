package org.alex73.korpus.compiler.parsers;

import org.alex73.korpus.compiler.BaseParallelProcessor;

public interface IParser {
    void parse(BaseParallelProcessor processor, boolean headersOnly) throws Exception;
}
