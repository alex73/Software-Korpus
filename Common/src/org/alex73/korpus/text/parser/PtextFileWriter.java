package org.alex73.korpus.text.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
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
 * Writer for files with grammar info.
 */
public class PtextFileWriter {
    protected static final char MIN_CONTROL_CHAR = '\u2190';
    protected static final char MAX_CONTROL_CHAR = '\u21FF';
    protected static final char SENTENCE_SEPARATOR = '\u21B3';
    protected static final char START_WORD = '\u21B7';
    protected static final char START_WORD_NORMALIZED = '\u21B6';
    protected static final char START_WORD_LEMMA = '\u21F8';
    protected static final char START_WORD_TAG = '\u21FB';
    protected static final char START_WORD_TYPE = '\u21F9';
    protected static final char START_TAIL = '\u21D2';
    protected static final char START_LONG_TAG = '\u21C5';
    protected static final char START_SHORT_TAG = '\u21C4';

    public static void write(File outFile, Map<String, String> headers, List<TextLine> text) throws Exception {
        try (BufferedWriter wr = Files.newBufferedWriter(outFile.toPath(), StandardCharsets.UTF_8)) {
            TextFileWriter.writeHeaders(wr, headers);

            for (TextLine line : text) {
                for (ITextLineElement it : line) {
                    if (it instanceof WordItem) {
                        WordItem wi = (WordItem) it;
                        write(wr, START_WORD, wi.lightNormalized);
                        if (wi.manualNormalized != null) {
                            write(wr, START_WORD_NORMALIZED, wi.manualNormalized);
                        }
                        if (wi.manualLemma != null) {
                            write(wr, START_WORD_LEMMA, wi.manualLemma);
                        }
                        if (wi.manualTag != null) {
                            write(wr, START_WORD_TAG, wi.manualTag);
                        }
                        if (wi.type != null) {
                            write(wr, START_WORD_TYPE, wi.type.name());
                        }
                    } else if (it instanceof TailItem) {
                        if (it == line.get(line.size() - 1)) {
                            // latest - check newline
                            String tt = it.getText();
                            if (tt.equals("\n")) {
                                // nothing
                            } else if (tt.endsWith("\n")) {
                                // except last
                                write(wr, START_TAIL, tt.substring(0, tt.length() - 1));
                            }
                        } else {
                            write(wr, START_TAIL, it.getText());
                        }
                    } else if (it instanceof SentenceSeparatorItem) {
                        wr.write(SENTENCE_SEPARATOR);
                    } else if (it instanceof LongTagItem) {
                        write(wr, START_LONG_TAG, it.getText());
                    } else if (it instanceof InlineTag) {
                        write(wr, START_SHORT_TAG, it.getText());
                    } else {
                        throw new RuntimeException("Unknown item type: " + it.getClass());
                    }
                }
                wr.write("\n");
            }
        }
    }

    static void write(BufferedWriter wr, char c, String str) throws IOException {
        if (str == null || str.isEmpty()) {
            throw new RuntimeException("Required string is empty");
        }
        checkChars(str);
        wr.write(c);
        wr.write(str);
    }

    static void checkChars(String str) throws IOException {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= MIN_CONTROL_CHAR && c <= MAX_CONTROL_CHAR) {
                throw new RuntimeException("Illegal char in text: " + str);
            }
        }
    }
}
