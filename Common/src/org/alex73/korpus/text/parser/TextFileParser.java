package org.alex73.korpus.text.parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.text.structure.files.LongTagItem;
import org.alex73.korpus.text.structure.files.TailItem;
import org.alex73.korpus.text.structure.files.TextLine;

/**
 * Parser for palicka-like text files, but with multiple texts in one file
 * support. It parses only line-by-line. See PtextToKorpus for poetry and
 * 'empty-line-separator' modes.
 */
public class TextFileParser {
    static final Pattern RE_TEXTS_SEPARATOR = Pattern.compile("#{3,}");
    public final List<OneText> texts = new ArrayList<>();
    private Headers globalHeaders;
    private final List<String> textLines = new ArrayList<>();
    private int currentLine;

    public class OneText {
        public final Headers headers;
        public final List<String> sourceLines = new ArrayList<>();
        public final List<TextLine> lines = new ArrayList<>();

        OneText() throws Exception {
            headers = readHeaders();
            headers.appendDefaultHeaders(globalHeaders);

            String s;
            while (currentLine < textLines.size()) {
                s = textLines.get(currentLine++);
                if (RE_TEXTS_SEPARATOR.matcher(s).matches()) {
                    break;
                }
                sourceLines.add(s);
            }
        }

        public void parse(ILanguage lang, boolean processSimpleHtml, IProcess errors) {
            Splitter3 splitter = new Splitter3(lang.getNormalizer(), processSimpleHtml, errors);
            for (String s : sourceLines) {
                if (s.trim().isEmpty()) {
                    TextLine line = new TextLine();
                    line.add(new TailItem(s + "\n"));
                    lines.add(line);
                    continue;
                }
                if (s.startsWith("##")) {
                    TextLine p;
                    int pos = s.indexOf(':');
                    String after = s.substring(pos + 1).trim();
                    if (after.equals("begin") || after.equals("end") || s.startsWith("##Page:")) {
                        p = new TextLine();
                        p.add(new LongTagItem(s));
                    } else {
                        p = splitter.parse(s.substring(pos + 1));
                        p.add(0, new LongTagItem(s.substring(0, pos + 1)));
                    }
                    lines.add(p);
                    continue;
                }
                TextLine p = splitter.parse(s);
                p.add(new TailItem("\n"));
                lines.add(p);
            }
        }
    }

    public TextFileParser(InputStream in, boolean headersOnly) {
        try {
            BufferedReader rd = new BOMBufferedReader(new InputStreamReader(in, "UTF-8"));
            String s;
            // read full text
            while ((s = rd.readLine()) != null) {
                textLines.add(s);
            }

            globalHeaders = readHeaders();
            boolean wasGlobalHeaders = false;
            for (int i = Math.max(0, currentLine - 1); i < textLines.size(); i++) {
                String line = textLines.get(i);
                if (!line.isBlank()) {
                    if (RE_TEXTS_SEPARATOR.matcher(line).matches()) {
                        wasGlobalHeaders = true;
                    }
                    break;
                }
            }
            if (!wasGlobalHeaders) {
                currentLine = 0;
                globalHeaders = new Headers();
            }

            while (currentLine < textLines.size()) {
                texts.add(new OneText());
            }
        } catch (Exception ex) {
            throw new RuntimeException("Памылка разбору ў радку #" + currentLine, ex);
        }
    }

    public OneText oneTextExpected() {
        if (texts.size() != 1) {
            throw new RuntimeException("Expected exact one text, but found " + texts.size());
        }
        return texts.get(0);
    }

    private Headers readHeaders() throws Exception {
        Headers headers = new Headers();
        while (currentLine < textLines.size()) {
            String s = textLines.get(currentLine++);
            if (s.isBlank()) {
                break;
            }
            headers.add(s);
        }
        return headers;
    }

    public static Headers readHeaders(BufferedReader rd) throws Exception {
        Headers headers = new Headers();
        String s;
        while ((s = rd.readLine()) != null) {
            if (s.isBlank()) {
                break;
            }
            headers.add(s);
        }
        return headers;
    }
}
