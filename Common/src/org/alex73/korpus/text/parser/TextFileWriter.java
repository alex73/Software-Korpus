package org.alex73.korpus.text.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

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
    public static void write(File outFile, Map<String, String> headers, List<TextLine> text) throws Exception {
        try (BufferedWriter wr = Files.newBufferedWriter(outFile.toPath(), StandardCharsets.UTF_8)) {
            TextFileWriter.writeHeaders(wr, headers);

            for (TextLine line : text) {
                for (ITextLineElement it : line) {
                    if (it instanceof WordItem) {
                        WordItem wi = (WordItem) it;
                        wr.write(wi.lightNormalized);
                    } else if (it instanceof TailItem) {
                        if (it == line.get(line.size() - 1)) {
                            // latest - check newline
                            String tt = it.getText();
                            if (tt.equals("\n")) {
                                // nothing
                            } else if (tt.endsWith("\n")) {
                                // except last
                                wr.write(tt.substring(0, tt.length() - 1));
                            }
                        } else {
                            wr.write(it.getText());
                        }
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

    public static void writeHeaders(BufferedWriter wr, Map<String, String> headers) throws Exception {
        for (Map.Entry<String, String> h : headers.entrySet()) {
            if (h.getValue().contains("\n")) {
                wr.write("##" + h.getKey() + "_BEGIN\n" + h.getValue() + "\n" + h.getKey() + "_END\n");
            } else {
                wr.write("##" + h.getKey() + ": " + h.getValue() + "\n");
            }
        }
        wr.write("\n");
    }
}
