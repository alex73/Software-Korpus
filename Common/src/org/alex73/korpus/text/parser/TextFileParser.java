package org.alex73.korpus.text.parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.alex73.korpus.text.structure.files.LongTagItem;
import org.alex73.korpus.text.structure.files.TailItem;
import org.alex73.korpus.text.structure.files.TextLine;

/**
 * Parser for palicka-like text files.
 * It parses only line-by-line. See PtextToKorpus for poetry and 'empy-line-separator' modes.
 */
public class TextFileParser {
    public final Headers headers;
    public final List<String> sourceLines = new ArrayList<>();
    public final List<TextLine> lines = new ArrayList<>();
    private Splitter3 splitter;

    public TextFileParser(InputStream in, boolean headersOnly) {
        try {
            BufferedReader rd = new BOMBufferedReader(new InputStreamReader(in, "UTF-8"));

            headers = readHeaders(rd);
            if (headersOnly) {
                return;
            }

            String s;
            while ((s = rd.readLine()) != null) {
                sourceLines.add(s);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void parse(IProcess errors) {
        splitter = new Splitter3(true, errors);

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
