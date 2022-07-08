package org.alex73.korpus.text.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.alex73.korpus.text.structure.files.ITextLineElement;
import org.alex73.korpus.text.structure.files.InlineTag;
import org.alex73.korpus.text.structure.files.LongTagItem;
import org.alex73.korpus.text.structure.files.SentenceSeparatorItem;
import org.alex73.korpus.text.structure.files.TailItem;
import org.alex73.korpus.text.structure.files.TextLine;
import org.alex73.korpus.text.structure.files.WordItem;

/**
 * Writer for usual text files. Just for compare with source text.
 */
public class TextFileWriter {
    public static void write(File outFile, Headers headers, List<TextLine> text) throws Exception {
        try (BufferedWriter wr = Files.newBufferedWriter(outFile.toPath(), StandardCharsets.UTF_8)) {
            TextFileWriter.writeHeaders(wr, headers);

            for (TextLine line : text) {
                for (ITextLineElement it : line) {
                    if (it instanceof WordItem) {
                        WordItem wi = (WordItem) it;
                        wr.write(Splitter3.encodeString(wi.lightNormalized));
                    } else if (it instanceof TailItem) {
                        String tt = it.getText();
                        if (it == line.get(line.size() - 1)) {
                            // latest - check newline
                            if (tt.endsWith("\n")) {
                                // except last
                                tt = tt.substring(0, tt.length() - 1);
                            }
                        }
                        wr.write(Splitter3.encodeString(tt));
                    } else if (it instanceof SentenceSeparatorItem) {
                    } else if (it instanceof LongTagItem) {
                        wr.write(it.getText());
                    } else if (it instanceof InlineTag) {
                        wr.write(it.getText());
                    } else {
                        throw new RuntimeException("Unknown item type: " + it.getClass());
                    }
                }
                wr.write("\n");
            }
        }
    }

    public static void writeHeaders(Writer wr, Headers headers) throws Exception {
        for (String s : headers.headers) {
            wr.write(s + "\n");
        }
        wr.write("\n");
    }
}
