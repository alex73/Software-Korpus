package org.alex73.korpus.compiler;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.alex73.korpus.compiler.parsers.IParser;
import org.alex73.korpus.compiler.parsers.ParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesReader extends BaseParallelProcessor<Path> {
    private static final Logger LOG = LoggerFactory.getLogger(FilesReader.class);

    private final Path inputDirectory;
    private final boolean headersOnly;
    private final Consumer<MessageParsedText> processParser;

    public FilesReader(Path inputDirectory, boolean headersOnly, Consumer<MessageParsedText> processParser) throws Exception {
        super(8, 16);
        this.inputDirectory = inputDirectory;
        this.headersOnly = headersOnly;
        this.processParser = processParser;
        List<Path> files = Files.find(inputDirectory, Integer.MAX_VALUE, (p, a) -> a.isRegularFile(), FileVisitOption.FOLLOW_LINKS)
                .sorted((p1, p2) -> {
                    try {
                        return Long.compare(Files.size(p2), Files.size(p1));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }).toList();
        files.forEach(p -> accept(p));
    }

    @Override
    public void accept(Path file) {
        run(() -> {
            LOG.trace("Read file " + file);
            String rel = inputDirectory.relativize(file).toString();
            IParser parser = ParserFactory.getParser(rel, file);
            if (parser == null) {
                throw new Exception("Unknown parser for " + rel, null);
            } else {
                parser.parse(processParser, headersOnly);
            }
            LOG.trace("Finished file " + file);
        });
    }
}
