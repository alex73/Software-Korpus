package org.alex73.korpus.compiler;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;

import org.alex73.korpus.compiler.parsers.IParser;
import org.alex73.korpus.compiler.parsers.ParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesReader extends BaseParallelProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(FilesReader.class);

    private final Path inputDirectory;
    private final boolean headersOnly;
    private final BaseParallelProcessor processParser;

    public FilesReader(Path inputDirectory, boolean headersOnly, BaseParallelProcessor processParser) throws Exception {
        super(8, 10);
        this.inputDirectory = inputDirectory;
        this.headersOnly = headersOnly;
        this.processParser = processParser;
        Files.find(inputDirectory, Integer.MAX_VALUE, (p, a) -> a.isRegularFile(), FileVisitOption.FOLLOW_LINKS)
                .sorted((p1, p2) -> {
                    try {
                        return Long.compare(Files.size(p2), Files.size(p1));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }).forEach(p -> process(p));
    }

    protected void process(Path file) {
        run(() -> {
            LOG.trace("Read file " + file);
            String rel = inputDirectory.relativize(file).toString();
            int p = rel.indexOf('/');
            if (p < 0) {
                return;
            }
            String currentSubcorpus = rel.substring(0, rel.indexOf('/'));
            IParser parser = ParserFactory.getParser(currentSubcorpus, file);
            if (parser == null) {
                throw new Exception("Unknown parser for " + rel, null);
            } else {
                parser.parse(processParser, headersOnly);
            }
            LOG.trace("Finished file " + file);
        });
    }
}
