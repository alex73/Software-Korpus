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

import org.alex73.korpus.text.elements.Paragraph;

public class TextFileParser {
    enum BLOCK_MODE {
        ONE_LINE, POETRY, PAGE
    };

    private static Pattern RE_TAG = Pattern.compile("##([A-Za-z0-9]+):?(.*)");
    private static Pattern RE_TAG_BEGIN = Pattern.compile("##([A-Za-z0-9]+)_BEGIN");

    public final Map<String, String> headers = new TreeMap<>();
    public final List<Paragraph> paragraphs = new ArrayList<>();
    private final StringBuilder buffer = new StringBuilder();
    private Splitter3 splitter;
    private BLOCK_MODE mode = BLOCK_MODE.ONE_LINE;
    private int page;

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
                if (s.isEmpty()) {
                    switch (mode) {
                    case ONE_LINE:
                        continue;
                    case POETRY:
                        flushBuffer();
                        continue;
                    default:
                        break;
                    }
                }
                if (s.startsWith("##")) {
                    s = s.replace(" ", "");
                    if ("##Poetry:begin".equals(s)) {
                        mode = BLOCK_MODE.POETRY;
                    } else if ("##Poetry:end".equals(s)) {
                        flushBuffer();
                        mode = BLOCK_MODE.ONE_LINE;
                    } else if (s.startsWith("##Page:")) {
                        flushBuffer();
                        mode = BLOCK_MODE.PAGE;
                        page = Integer.parseInt(s.substring(7));
                    }
                    continue;
                } else {
                    buffer.append(s).append('\n');
                }
                if (mode == BLOCK_MODE.ONE_LINE) {
                    Paragraph p = splitter.parse(s);
                    p.page = page;
                    paragraphs.add(p);
                    buffer.setLength(0);
                }
            }
            flushBuffer();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void flushBuffer() {
        if (buffer.length() > 0) {
            Paragraph p = splitter.parse(buffer);
            p.page = page;
            paragraphs.add(p);
            buffer.setLength(0);
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
