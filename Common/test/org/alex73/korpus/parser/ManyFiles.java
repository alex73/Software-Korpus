package org.alex73.korpus.parser;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.alex73.korpus.text.parser.PtextFileParser;
import org.alex73.korpus.text.parser.PtextFileWriter;
import org.alex73.korpus.text.parser.TextFileParser;
import org.alex73.korpus.text.parser.TextFileWriter;
import org.junit.Test;

public class ManyFiles {
    static final String FILES = "test-files/";
    static final String TEMP = "/tmp/text.tmp";

    @Test
    public void preserveFormat() throws Exception {
        try (Stream<Path> stream = Files.find(Paths.get(FILES), Integer.MAX_VALUE, (p, a) -> a.isRegularFile()).sorted()) {
            for (Path f : stream.toList()) {
                System.out.println(f);
                List<String> expected = Files.readAllLines(f);
                try (InputStream in = Files.newInputStream(f)) {
                    TextFileParser text = new TextFileParser(in, false);
                    text.parse(null);
                    TextFileWriter.write(new File(TEMP), text.headers, text.lines);
                    List<String> created = Files.readAllLines(Paths.get(TEMP));
                    assertEquals(expected, created);

                    PtextFileWriter.write(new File(TEMP + "p"), text.headers, text.lines);
                }

                try (InputStream in = Files.newInputStream(Paths.get(TEMP + "p"))) {
                    PtextFileParser text = new PtextFileParser(in, false, null);
                    TextFileWriter.write(new File(TEMP), text.headers, text.lines);
                    List<String> created = Files.readAllLines(Paths.get(TEMP));
                    assertEquals(expected, created);
                }
            }
        }
    }
}
