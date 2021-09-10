package org.alex73.korpus.text.parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alex73.korpus.text.structure.files.LongTagItem;
import org.alex73.korpus.text.structure.files.TailItem;
import org.alex73.korpus.text.structure.files.TextLine;

/**
 * Parser for palicka-like text files.
 * It parses only line-by-line. See PtextToKorpus for poetry and 'empy-line-separator' modes.
 */
public class TextFileParser {
    private static Pattern RE_TAG = Pattern.compile("##([A-Za-z0-9]+):?(.*)");
    private static Pattern RE_TAG_BEGIN = Pattern.compile("##([A-Za-z0-9]+)_BEGIN");

    public final Map<String, String> headers;
    public final List<TextLine> lines = new ArrayList<>();
    private Splitter3 splitter;

    public TextFileParser(InputStream in, boolean headersOnly, IProcess errors) {
        splitter = new Splitter3(true, errors);
        try {
            BufferedReader rd = new BOMBufferedReader(new InputStreamReader(in, "UTF-8"));

            headers = readHeaders(rd);
            if (headersOnly) {
                return;
            }

            String s;
            while ((s = rd.readLine()) != null) {
                if (s.trim().isEmpty()) {
                    TextLine line = new TextLine();
                    line.add(new TailItem("\n"));
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
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Map<String, String> readHeaders(BufferedReader rd) throws Exception {
        Map<String, String> headers = new TreeMap<>();
        String s;
        while ((s = rd.readLine()) != null) {
            if (s.trim().length() == 0) {
                break;
            }
            Matcher m;
            if ((m = RE_TAG_BEGIN.matcher(s)).matches()) {
                headers.put(m.group(1), readMultilineTag(m.group(1), rd));
            } else if ((m = RE_TAG.matcher(s)).matches()) {
                if (headers.containsKey(m.group(1))) {
                    throw new ParseException("Загаловак '##" + m.group(1) + "' вызначаны двойчы", -1);
                }
                headers.put(m.group(1), m.group(2).trim());
            } else {
                throw new RuntimeException("Няправільны загаловак '" + s + "'");
            }
        }
        return headers;
    }

    private static String readMultilineTag(String tagName, BufferedReader rd) throws Exception {
        String out = "";

        String endLine = "##" + tagName + "_END";
        String s;
        while ((s = rd.readLine()) != null) {
            if (s.equals(endLine)) {
                return out;
            }
            out += s + "\n";
        }
        throw new Exception("Wrong description header");
    }
}
