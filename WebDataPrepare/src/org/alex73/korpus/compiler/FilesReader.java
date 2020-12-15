package org.alex73.korpus.compiler;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.alex73.korpus.compiler.parsers.IParser;
import org.alex73.korpus.compiler.parsers.ParserFactory;
import org.alex73.korpus.text.parser.IProcess;

public class FilesReader {
    private final ThreadPoolExecutor parsers;

    public FilesReader() {
        parsers = new ThreadPoolExecutor(32, 32, 1, TimeUnit.DAYS, new LinkedBlockingQueue<>(512));
        parsers.setRejectedExecutionHandler((task, executor) -> {
            try {
                executor.getQueue().put(task);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Executor was interrupted", ex);
            }
        });
    }

    public void run(Path inputDirectory, IProcess errors) throws Exception {
        try {
            Files.find(inputDirectory, Integer.MAX_VALUE, (p, a) -> a.isRegularFile(), FileVisitOption.FOLLOW_LINKS)
                    .sorted().forEach(p -> {
                        String rel = inputDirectory.relativize(p).toString();
                        String currentSubcorpus = rel.substring(0, rel.indexOf('/'));
                        IParser parser = ParserFactory.getParser(currentSubcorpus, p);
                        if (parser == null) {
                            errors.reportError("Unknown parser for " + rel, null);
                        } else {
                            try {
                                parser.parse(parsers);
                            } catch (Exception ex) {
                                errors.reportError(ex.getMessage() + ": " + p, ex);
                            }
                        }
                    });
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        parsers.shutdown();
        parsers.awaitTermination(4, TimeUnit.HOURS);
    }
}
