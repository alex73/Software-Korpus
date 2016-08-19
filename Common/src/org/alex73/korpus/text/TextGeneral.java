package org.alex73.korpus.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alex73.korpus.text.parser.BOMBufferedReader;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.parser.Splitter2;
import org.alex73.korpus.text.xml.Content;
import org.alex73.korpus.text.xml.Header;
import org.alex73.korpus.text.xml.ITextLineElement;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.Poetry;
import org.alex73.korpus.text.xml.S;
import org.alex73.korpus.text.xml.Se;
import org.alex73.korpus.text.xml.Tag;
import org.alex73.korpus.text.xml.XMLText;

/**
 * Text with header, poetry tags and some simple html tags.
 */
public class TextGeneral implements IText {

    private static Pattern RE_TAG = Pattern.compile("##([A-Za-z0-9]+):?(.*)");
    private static Pattern RE_TAG_BEGIN = Pattern.compile("##([A-Za-z0-9]+)_BEGIN");

    private XMLText doc;

    public TextGeneral(File f, IProcess errors) throws Exception {
        try (InputStream in = new FileInputStream(f)) {
            load(in, errors);
        }
    }

    public TextGeneral(InputStream in, IProcess errors) throws Exception {
        load(in, errors);
    }

    private void load(InputStream in, IProcess errors) throws Exception {
        BufferedReader rd = new BOMBufferedReader(new InputStreamReader(in, "UTF-8"));

        Map<String, String> headers = extractHeaders(rd);

        doc = new XMLText();
        doc.setHeader(new Header());
        doc.setContent(new Content());

        for (Map.Entry<String, String> en : headers.entrySet()) {
            Tag t = new Tag();
            t.setName(en.getKey());
            t.setValue(en.getValue());
            doc.getHeader().getTag().add(t);
        }

        String s;
        while ((s = rd.readLine()) != null) {
            s = s.trim();
            if (s.startsWith("##")) {
                if (isPoetryStart(s)) {
                    Poetry p = addPoetry(rd, doc, s, errors);
                    doc.getContent().getPOrTagOrPoetry().add(p);
                } else {
                    Matcher m = RE_TAG.matcher(s);
                    if (!m.matches()) {
                        throw new RuntimeException("Няправільны тэг: " + s);
                    }
                    doc.getContent().getPOrTagOrPoetry().add(new Tag(m.group(1), m.group(2).trim()));
                }
            } else {
                P p = new Splitter2(s, true, errors).getP();
                doc.getContent().getPOrTagOrPoetry().add(p);
            }
        }
    }

    @Override
    public XMLText parse() {
        return doc;
    }

    static Poetry addPoetry(BufferedReader rd, XMLText doc, String s, IProcess errors) throws IOException {
        Poetry poetry = new Poetry();
        P p = null;
        Se se = null;

        while ((s = rd.readLine()) != null) {
            s = s.trim();
            if (s.isEmpty()) {
                // empty line - new paragraph
                if (p != null) {
                    p = null;
                    se = null;
                }
            } else if (s.startsWith("##")) {
                if (isPoetryEnd(s)) {
                    return poetry;
                } else {
                    Matcher m = RE_TAG.matcher(s);
                    if (!m.matches()) {
                        throw new RuntimeException("Няправільны тэг: " + s);
                    }
                    poetry.getPOrTag().add(new Tag(m.group(1), m.group(2).trim()));
                    p = null;
                    se = null;
                }
            } else {
                Splitter2 sp2 = new Splitter2(s, true, errors);
                P p2 = sp2.getP();
                boolean first = true;
                for (Se se2 : p2.getSe()) {
                    if (first) {
                        first = false;
                    } else {
                        se = new Se();
                    }
                    for (ITextLineElement it2 : se2.getWOrSOrZ()) {
                        if (p == null) {
                            p = new P();
                            poetry.getPOrTag().add(p);
                        }
                        if (se == null) {
                            se = new Se();
                            p.getSe().add(se);
                        }
                        se.getWOrSOrZ().add(it2);
                    }
                }
                se.getWOrSOrZ().add(new S('\n'));
                if (sp2.isSentenceFinished()) {
                    se = null;
                }
            }
        }

        return poetry;
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

    private static Map<String, String> extractHeaders(BufferedReader rd) throws Exception {
        Map<String, String> result = new TreeMap<String, String>();

        String s;
        while ((s = rd.readLine()) != null) {
            if (s.trim().length() == 0) {
                break;
            }
            Matcher m;
            if ((m = RE_TAG_BEGIN.matcher(s)).matches()) {
                result.put(m.group(1), readMultilineTag(m.group(1), rd));
            } else if ((m = RE_TAG.matcher(s)).matches()) {
                if (!m.group(1).startsWith("Hidden")) {
                    if (result.containsKey(m.group(1))) {
                        throw new ParseException("Загаловак '##" + m.group(1) + "' вызначаны двойчы", -1);
                    }
                    result.put(m.group(1), m.group(2).trim());
                }
            } else {
                throw new RuntimeException("Няправільны загаловак '" + s + "'");
            }
        }

        return result;
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
