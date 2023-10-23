package org.alex73.korpus.parsers.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for html files from knihi.com.
 */
public class HtmlKnihiComParser {
    static final Pattern RE_HEADER = Pattern.compile("<\\!\\-\\- HEADER_FIELD (\\S+): (.+) \\-\\->");
    static final String BOOK_BEGIN = "<!-- BOOK_BEGIN -->";
    static final String BOOK_END = "<!-- BOOK_END -->";
    static final String POETRY_BEGIN = "<!-- POETRY_BEGIN -->";
    static final String POETRY_END = "<!-- POETRY_END -->";
    static final String EPIGRAPH_BEGIN = "<!-- EPIGRAPH_BEGIN -->";
    static final String EPIGRAPH_END = "<!-- EPIGRAPH_END -->";
    static final String QUOTE_BEGIN = "<!-- QUOTE_BEGIN -->";
    static final String QUOTE_END = "<!-- QUOTE_END -->";
    static final String HTML_BEGIN = "<!-- HTML_BEGIN -->";
    static final String HTML_END = "<!-- HTML_END -->";
    public Map<String, String> headers = new TreeMap<>();
    public final List<String> textLines = new ArrayList<>();

    public HtmlKnihiComParser(InputStream in) throws Exception {
        List<String> lines = new ArrayList<>();

        BufferedReader rd = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        String s;
        // read full text
        while ((s = rd.readLine()) != null) {
            lines.add(s);
        }

        for (String line : lines) {
            Matcher m = RE_HEADER.matcher(line);
            if (m.matches()) {
                headers.put(m.group(1), m.group(2));
            }
        }
        if (headers.isEmpty()) {
            return;
        }

        boolean bookStarted = false;
        boolean htmlStarted = false;
        boolean poetryStarted = false;
        String poetryBlock = "";
        for (String line : lines) {
            switch (line) {
            case BOOK_BEGIN:
                bookStarted = true;
                continue;
            case BOOK_END:
                bookStarted = false;
                continue;
            case HTML_BEGIN:
                htmlStarted = true;
                continue;
            case HTML_END:
                htmlStarted = false;
                continue;
            case POETRY_BEGIN:
                poetryStarted = true;
                continue;
            case POETRY_END:
                if (!poetryBlock.isBlank()) {
                    textLines.add(poetryBlock);
                    poetryBlock = "";
                }
                poetryStarted = false;
                continue;
            case EPIGRAPH_BEGIN:
            case EPIGRAPH_END:
            case QUOTE_BEGIN:
            case QUOTE_END:
                continue;
            }
            if (!bookStarted || htmlStarted) {
                continue;
            }
            line = line.replaceAll("</?div[^>]*>", "");
            if (line.isBlank()) {
                continue;
            }
            line = parseLine(line);
            if (poetryStarted) {
                if (!line.replace("&nbsp;", " ").isBlank()) {
                    textLines.add(poetryBlock);
                    poetryBlock = "";
                } else {
                    poetryBlock += line + "\n";
                }
            } else {
                textLines.add(line);
            }
        }
    }

    static String parseLine(String s) {
        s = s.replaceAll("<a(.+?)</a>", "").replaceAll("</?p[^>]*>", "").replaceAll("</?h[0-9]>", "").replaceAll("<su[bp](.+?)</su[bp]>", "")
                .replaceAll("</?span[^>]*>", "").replaceAll("<img[^>]+/>", "").replaceAll("<hr[^>]+/>", "").replaceAll("</?center>", "");
        if (s.replaceAll("</?[bi]>", "").indexOf('<') >= 0) {
            throw new RuntimeException("Unknown tag: " + s);
        }
        return s;
    }
}
