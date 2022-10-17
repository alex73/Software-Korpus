package org.alex73.korpus.compiler.parsers;

import java.nio.file.Path;
import java.util.function.Consumer;

import org.alex73.korpus.compiler.MessageParsedText;

public class NullParser extends BaseParser {
    public NullParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(Consumer<MessageParsedText> subscriber, boolean headersObly) throws Exception {
    }
}
