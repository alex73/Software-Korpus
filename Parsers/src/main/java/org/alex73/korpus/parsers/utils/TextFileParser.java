package org.alex73.korpus.parsers.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.alex73.korpus.base.Ctf.Page;
import org.alex73.korpus.utils.BOMBufferedReader;

/**
 * Parser for like text files with headers, but with multiple texts in one file
 * support. It parses only line-by-line. See PtextToKorpus for poetry and
 * 'empty-line-separator' modes.
 */
public class TextFileParser {
    static final Pattern RE_TEXTS_SEPARATOR = Pattern.compile("#{3,}");
    public final List<OneText> texts = new ArrayList<>();
    private TextFileHeaders globalHeaders;
    private final List<String> textLines = new ArrayList<>();
    private int currentLine;

    public class OneText {
        public final TextFileHeaders headers;
        public final List<Page> pages = new ArrayList<>();
        private final List<String> paragraphs = new ArrayList<>();
        private String pageNum;
        private boolean inHTML;

        OneText() throws Exception {
            headers = readHeaders();
            headers.appendDefaultHeaders(globalHeaders);

            StringBuilder p = null;
            String s;
            while (currentLine < textLines.size()) {
                s = textLines.get(currentLine++);
                if (RE_TEXTS_SEPARATOR.matcher(s).matches()) {
                    break;
                }
                if (inHTML) {
                    if (s.trim().equals("##HTML:end")) {
                        inHTML = false;
                    }
                    continue;
                }
                if (s.isBlank()) {
                    if (p != null) {
                        flushParagraph(p);
                    }
                    continue;
                }
                if (s.startsWith("##")) {
                    switch (s.trim()) {
                    case "##Poetry:begin":
                        p = new StringBuilder();
                        continue;
                    case "##Poetry:end":
                        if (p == null) {
                            throw new RuntimeException("Няправільны фармат: ##Poetry:end без ##Poetry:begin у радку #" + currentLine);
                        }
                        flushParagraph(p);
                        p = null;
                        continue;
                    case "##Epigraph:begin":
                    case "##Epigraph:end":
                    case "##Quote:begin":
                    case "##Quote:end":
                    case "##Divider":
                        continue;
                    case "##HTML:begin":
                        inHTML = true;
                        continue;
                    }
                    if (s.startsWith("##Footnote:") || s.startsWith("##Endnote:") || s.startsWith("##AddImage:")) {
                        continue;
                    }
                    if (s.startsWith("##Page:")) {
                        flushPage();
                        pageNum = s.substring(s.indexOf(':') + 1).trim();
                        continue;
                    }
                    if (s.startsWith("##Center:") || s.startsWith("##Sign:") || s.matches("##(Sub)*Chapter:.+") || s.startsWith("##TheatreComment:")) {
                        s = s.substring(s.indexOf(':') + 1).trim();
                    }
                    if (s.startsWith("##") && s.matches("##[A-Za-z0-9]+:.+")) {
                        throw new RuntimeException("Невядомы тэг " + s + " у радку #" + currentLine);
                    }
                }

                if (p != null) {
                    if (p.length() > 0) {
                        p.append('\n');
                    }
                    p.append(s);
                } else {
                    s = s.trim();
                    if (!s.isEmpty()) {
                        paragraphs.add(s);
                    }
                }
            }
            if (p != null) {
                throw new RuntimeException("Незакрыты блок ##Poetry у радку #" + currentLine);
            }
            flushPage();
        }

        private void flushPage() {
            if (!paragraphs.isEmpty()) {
                Page p = new Page();
                p.pageNum = pageNum;
                p.paragraphs = paragraphs.toArray(new String[0]);
                pages.add(p);
                paragraphs.clear();
            }
        }

        private void flushParagraph(StringBuilder p) {
            String pp = p.toString().trim();
            if (!pp.isEmpty()) {
                paragraphs.add(pp);
            }
            p.setLength(0);
        }

    }

    public TextFileParser(InputStream in) {
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
                globalHeaders = new TextFileHeaders();
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

    private TextFileHeaders readHeaders() throws Exception {
        TextFileHeaders headers = new TextFileHeaders();
        currentLine = headers.parse(textLines, currentLine);
        return headers;
    }
}
