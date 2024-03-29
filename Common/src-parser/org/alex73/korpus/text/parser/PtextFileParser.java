package org.alex73.korpus.text.parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.alex73.korpus.text.structure.corpus.Word.OtherType;
import org.alex73.korpus.text.structure.files.InlineTag;
import org.alex73.korpus.text.structure.files.LongTagItem;
import org.alex73.korpus.text.structure.files.SentenceSeparatorItem;
import org.alex73.korpus.text.structure.files.TailItem;
import org.alex73.korpus.text.structure.files.TextLine;
import org.alex73.korpus.text.structure.files.WordItem;
import org.alex73.korpus.utils.BOMBufferedReader;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Headers;

/**
 * Parser for files with grammar info.
 */
public class PtextFileParser {
    public final Headers headers;
    public final List<TextLine> lines = new ArrayList<>();
    private String s;
    private int pos;
    private TextLine line;
    private StringBuilder str = new StringBuilder();
    private String word, wNormalized, wManualLemma, wManualTag;
    private OtherType wType;

    public PtextFileParser(InputStream in, boolean headersOnly, IProcess errors) {
        try {
            BufferedReader rd = new BOMBufferedReader(new InputStreamReader(in, "UTF-8"));

            headers = TextFileParser.readHeaders(rd);
            if (headersOnly) {
                return;
            }

            while ((s = rd.readLine()) != null) {
                line = new TextLine();
                if (s.trim().isEmpty()) {
                    line.add(new TailItem(s + "\n"));
                    lines.add(line);
                    continue;
                } else {
                    pos = 0;
                    while (true) {
                        char c = next();
                        if (c == 0) {
                            break;
                        }
                        switch (c) {
                        case PtextFileWriter.SENTENCE_SEPARATOR:
                            flushWord();
                            line.add(new SentenceSeparatorItem());
                            break;
                        case PtextFileWriter.START_LONG_TAG:
                            flushWord();
                            line.add(new LongTagItem(readString()));
                            break;
                        case PtextFileWriter.START_SHORT_TAG:
                            flushWord();
                            line.add(new InlineTag(readString()));
                            break;
                        case PtextFileWriter.START_TAIL:
                            flushWord();
                            line.add(new TailItem(readString()));
                            break;
                        case PtextFileWriter.START_WORD:
                            flushWord();
                            word = readString();
                            break;
                        case PtextFileWriter.START_WORD_NORMALIZED:
                            wNormalized = readString();
                            break;
                        case PtextFileWriter.START_WORD_LEMMA:
                            wManualLemma = readString();
                            break;
                        case PtextFileWriter.START_WORD_TAG:
                            wManualTag = readString();
                            break;
                        case PtextFileWriter.START_WORD_TYPE:
                            wType = OtherType.valueOf(readString());
                            break;
                        default:
                            throw new RuntimeException("Unknown control char: \\u" + Integer.toHexString(c));
                        }
                    }
                }
                flushWord();
                line.add(new TailItem("\n"));
                lines.add(line);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private char next() {
        if (pos >= s.length()) {
            return 0;
        }
        char c = s.charAt(pos);
        pos++;
        return c;
    }

    private String readString() {
        str.setLength(0);
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c >= PtextFileWriter.MIN_CONTROL_CHAR && c <= PtextFileWriter.MAX_CONTROL_CHAR) {
                break;
            }
            pos++;
            str.append(c);
        }
        return str.toString();
    }

    private void flushWord() {
        if (word != null) {
            WordItem w = new WordItem();
            w.word = word;
            w.manualNormalized = wNormalized;
            w.manualLemma = wManualLemma;
            w.manualTag = wManualTag;
            w.type = wType;
            line.add(w);
            word = null;
            wNormalized = null;
            wManualLemma = null;
            wManualTag = null;
            wType = null;
        }
    }
}
