package org.alex73.korpus.text.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
 * Writer for files with grammar info.
 */
public class PtextFileWriter {
    protected static final char MIN_CONTROL_CHAR = '\u13A0';
    protected static final char MAX_CONTROL_CHAR = '\u13FF';
    protected static final char SENTENCE_SEPARATOR = '\u13D7'; // Ꮧ
    protected static final char START_WORD = '\u13D4'; // Ꮤ
    protected static final char START_WORD_NORMALIZED = '\u13E5'; // Ꮵ
    protected static final char START_WORD_LEMMA = '\u13DE'; // Ꮮ
    protected static final char START_WORD_TAG = '\u13A2'; // Ꭲ
    protected static final char START_WORD_TYPE = '\u13C7'; // Ꮗ
    protected static final char START_TAIL = '\u13ED'; // Ꮽ
    protected static final char START_LONG_TAG = '\u13BD'; // Ꮍ
    protected static final char START_SHORT_TAG = '\u13A9'; // Ꭹ

    public static void write(File outFile, Headers headers, List<TextLine> text) throws Exception {
        try (BufferedWriter wr = Files.newBufferedWriter(outFile.toPath(), StandardCharsets.UTF_8)) {
        //try (OutputStreamWriter wr =new OutputStreamWriter( Files.newOutputStream(outFile.toPath()), StandardCharsets.UTF_8)) {
//           Writer  wr=new PrintWriter(System.out);
            TextFileWriter.writeHeaders(wr, headers);

            for (TextLine line : text) {
                for (ITextLineElement it : line) {
                    if (it instanceof WordItem) {
                        WordItem wi = (WordItem) it;
                        write(wr, START_WORD, Splitter3.encodeString(wi.lightNormalized));
                        if (wi.manualNormalized != null) {
                            write(wr, START_WORD_NORMALIZED, Splitter3.encodeString(wi.manualNormalized));
                        }
                        if (wi.manualLemma != null) {
                            write(wr, START_WORD_LEMMA, Splitter3.encodeString(wi.manualLemma));
                        }
                        if (wi.manualTag != null) {
                            write(wr, START_WORD_TAG, wi.manualTag);
                        }
                        if (wi.type != null) {
                            write(wr, START_WORD_TYPE, wi.type.name());
                        }
                    } else if (it instanceof TailItem) {
                        String tt = it.getText();
                        if (it == line.get(line.size() - 1)) {
                            // latest - check newline
                            if (tt.endsWith("\n")) {
                                // except last
                                tt = tt.substring(0, tt.length() - 1);
                            }
                        }
                        if (!tt.isEmpty()) {
                            write(wr, START_TAIL, Splitter3.encodeString(tt));
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
                //System.out.println();
            }
        }
    }

    static void write(Writer wr, char c, String str) throws IOException {
        if (str == null || str.isEmpty()) {
            throw new RuntimeException("Required string is empty");
        }
        checkChars(str);
        wr.write(c);
        wr.write(str);
//        System.out.print(c+str);
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
