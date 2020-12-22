package org.alex73.korpus.text.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alex73.korpus.text.elements.Paragraph;

public class TextFileParser {

    private static Pattern RE_TAG = Pattern.compile("##([A-Za-z0-9]+):?(.*)");
    private static Pattern RE_TAG_BEGIN = Pattern.compile("##([A-Za-z0-9]+)_BEGIN");

    public final Map<String, String> headers = new TreeMap<>();
    public final List<Paragraph> paragraphs = new ArrayList<>();
    private final StringBuilder poetryBuffer = new StringBuilder();
    private Splitter3 splitter;

    public TextFileParser(InputStream in, boolean headersOnly, IProcess errors) {
        splitter = new Splitter3(true, errors);
        try {
            BufferedReader rd = new BOMBufferedReader(new InputStreamReader(in, "UTF-8"));

            readHeaders(rd);
            if (headersOnly) {
                return;
            }

            String s;
            while ((s = rd.readLine()) != null) {
                s = s.trim();
                if (s.isEmpty() && paragraphs.isEmpty()) {
                    continue;
                }
                if (s.startsWith("##")) {
                    if (isPoetryStart(s)) {
                        addPoetry(rd);
                    }
                } else {
                    paragraphs.add(splitter.parse(s));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    void addPoetry(BufferedReader rd) throws IOException {
        String s;
        while ((s = rd.readLine()) != null) {
            s = s.trim();
            if (s.isEmpty()) {
                // empty line - new paragraph
                flushPoetry();
            } else if (s.startsWith("##")) {
                if (isPoetryEnd(s)) {
                    flushPoetry();
                    return;
                }
            } else {
                poetryBuffer.append(s).append('\n');
            }
        }

        flushPoetry();
    }

    private void flushPoetry() {
        if (poetryBuffer.length() > 0) {
            paragraphs.add(splitter.parse(poetryBuffer));
            poetryBuffer.setLength(0);
        }
    }

    /**
     * Check is poetry tag starts.
     */
    static boolean isPoetryStart(String s) {
        Matcher m = RE_TAG.matcher(s);
        if (m.matches()) {
            return "Poetry".equals(m.group(1)) && "begin".equals(m.group(2).trim());
        } else {
            return false;
        }
    }

    /**
     * Check is poetry tag ends.
     */
    static boolean isPoetryEnd(String s) {
        Matcher m = RE_TAG.matcher(s);
        if (m.matches()) {
            return "Poetry".equals(m.group(1)) && "end".equals(m.group(2).trim());
        } else {
            return false;
        }
    }

    private void readHeaders(BufferedReader rd) throws Exception {
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
