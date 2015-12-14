/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2013 Aleś Bułojčyk (alex73mail@gmail.com)
               Home page: https://sourceforge.net/projects/korpus/

 This file is part of Korpus.

 Korpus is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Korpus is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.alex73.korpus.text.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Text file converter into text XML document.
 */
public class TextParser {

    public static XMLText parseText(InputStream in, boolean headerOnly, IProcess errors) throws Exception {
        BufferedReader rd = new BOMBufferedReader(new InputStreamReader(in, "UTF-8"));

        Map<String, String> headers = extractHeaders(rd);

        XMLText doc = new XMLText();
        doc.setHeader(new Header());
        doc.setContent(new Content());

        for (Map.Entry<String, String> en : headers.entrySet()) {
            Tag t = new Tag();
            t.setName(en.getKey());
            t.setValue(en.getValue());
            doc.getHeader().getTag().add(t);
        }
        if (headerOnly) {
            return doc;
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

    static Pattern RE_TAG = Pattern.compile("##([A-Za-z0-9]+):?\\s*(.*)");

    static Map<String, String> extractHeaders(BufferedReader rd) throws Exception {
        Map<String, String> result = new TreeMap<String, String>();

        String s;
        while ((s = rd.readLine()) != null) {
            if (s.trim().length() == 0) {
                break;
            }
            Matcher m = RE_TAG.matcher(s);
            if (m.matches()) {
                if (m.group(1).equals("Description") && m.group(2).trim().equals("begin")) {
                    result.put(m.group(1), readDescription(rd));
                } else if (!m.group(1).startsWith("Hidden")) {
                    if (result.containsKey(m.group(1))) {
                        throw new ParseException("Загаловак '##" + m.group(1) + "' вызначаны двойчы", -1);
                    }
                    result.put(m.group(1), m.group(2).trim());
                }
            } else {
                throw new RuntimeException("Няправільны загаловак '" + s);
            }
        }

        return result;
    }

    static String readDescription(BufferedReader rd) throws Exception {
        String out = "";

        String s;
        while ((s = rd.readLine()) != null) {
            Matcher m = RE_TAG.matcher(s);
            if (m.matches()) {
                if (m.group(1).equals("Description") && m.group(2).trim().equals("end")) {
                    return out;
                } else {
                    throw new Exception("Wrong description header");
                }
            } else {
                out += s + "\n";
            }
        }
        throw new Exception("Wrong description header");
    }
}
