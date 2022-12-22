package org.alex73.korpus.compiler.parsers;

import java.util.function.Consumer;

import org.alex73.korpus.compiler.MessageParsedText;

public interface IParser {
    void parse(Consumer<MessageParsedText> subscriber, boolean headersOnly) throws Exception;
}
