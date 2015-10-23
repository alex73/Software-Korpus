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

package org.alex73.korpus.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.alex73.korpus.editor.core.structure.Line;
import org.alex73.korpus.editor.core.structure.LongTagItem;
import org.alex73.korpus.editor.core.structure.SentenceSeparatorItem;

import alex73.corpus.text.Content;
import alex73.corpus.text.Header;
import alex73.corpus.text.P;
import alex73.corpus.text.S;
import alex73.corpus.text.Se;
import alex73.corpus.text.Tag;
import alex73.corpus.text.XMLText;

/**
 * Чытаньне тэкставага файлу й стварэньне дакумэнту корпуса.
 */
public class TextParser {
    public static JAXBContext CONTEXT;

    static {
        try {
            CONTEXT = JAXBContext.newInstance(XMLText.class.getPackage().getName());
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static XMLText parseXML(InputStream in) throws Exception {
        Unmarshaller unm = CONTEXT.createUnmarshaller();

        XMLText doc = (XMLText) unm.unmarshal(in);
        return doc;
    }

    public static void saveXML(File outFile, XMLText xml) throws Exception {
        Marshaller m = CONTEXT.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(xml, outFile);
    }

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

        boolean insidePoetry = false;
        String s = null;
        while (true) {
            while (s != null) {
                s = s.trim();
                if (s.startsWith("##")) {
                    Matcher m = RE_TAG.matcher(s);
                    if (!m.matches()) {
                        throw new RuntimeException("Wrong tag: " + s);
                    }
                    Tag t = new Tag();
                    t.setName(m.group(1));
                    t.setValue(m.group(2).trim());
                    doc.getContent().getPOrTag().add(t);
                    if ("##Poetry:begin".equals(s)) {
                        insidePoetry = true;
                    } else if ("##Poetry:end".equals(s)) {
                        insidePoetry = false;
                    }
                    s = null;
                } else if (insidePoetry)
                    s = addPoetry(rd, doc, s, errors);
                else {
                    P p = new Splitter2(s, true, errors).getP();
                    doc.getContent().getPOrTag().add(p);
                    s = null;
                }
            }
            s = rd.readLine();
            if (s == null) {
                break;
            }
        }

        return doc;
    }

    static String addPoetry(BufferedReader rd, XMLText doc, String s, IProcess errors) throws IOException {
        StringBuilder str = new StringBuilder(s.length() * 10);
        str.append(s).append('\n');

        while ((s = rd.readLine()) != null) {
            s = s.trim();
            if (s.startsWith("##")) {
                break;
            }
            str.append(s).append('\n');
            if (s.isEmpty()) {
                break;
            }
        }

        P p = new Splitter2(str.toString(), true, errors).getP();
        doc.getContent().getPOrTag().add(p);

        return s;
    }

    static final Pattern RE_DATE1 = Pattern.compile("([0-9]{4})");

    public static Integer parseDate(String date) {
        if (date == null) {
            return null;
        }

        Matcher m;
        if ((m = RE_DATE1.matcher(date)).matches()) {
            return Integer.parseInt(m.group(1));
        } else {
            throw new RuntimeException("Wrong date: " + date);
        }
    }

    public static List<Line> parseText(String text) throws Exception {
        List<Line> result = new ArrayList<>();

        BufferedReader rd = new BufferedReader(new StringReader(text));
        String s;
        while ((s = rd.readLine()) != null) {
            Splitter sp = new Splitter(s);
            result.add(sp.splitParagraph());
        }

        return result;
    }

    static Pattern RE_TAG = Pattern.compile("##([A-Za-z0-9]+):\\s*(.*)");

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

    public static XMLText constructXML(List<Line> doc) {
        XMLText text = new XMLText();
        text.setContent(new Content());

        P p = null;
        Se s = null;
        for (Line line : doc) {
            p = new P();
            s = new Se();
            for (int i = 0; i < line.size(); i++) {
                Object item = line.get(i);
                if (item instanceof SentenceSeparatorItem) {
                    p.getSe().add(s);
                    s = new Se();
                } else if (item instanceof LongTagItem) {
                    if (line.size() != 2 || i != 0) {
                        throw new RuntimeException("Памылка фармату LongTagItem1");
                    }
                    Object item2 = line.get(1);
                    if (!(item2 instanceof S) || !((S) item2).getChar().equals("\n")) {
                        throw new RuntimeException("Памылка фармату LongTagItem2");
                    }

                    Tag tag = new Tag();
                    Matcher m = RE_TAG.matcher(((LongTagItem) item).getText());
                    if (!m.matches()) {
                        throw new RuntimeException("Wrong long tag: " + ((LongTagItem) item).getText());
                    }
                    tag.setName(m.group(1));
                    tag.setValue(m.group(2).trim());
                    text.getContent().getPOrTag().add(tag);

                    p = null;
                    i++;
                } else {
                    if (i == line.size() - 1 && item instanceof S && ((S) item).getChar().equals("\n")) {
                        // skip
                    } else {
                        s.getWOrSOrZ().add(item);
                    }
                }
            }
            if (p != null) {
                p.getSe().add(s);
                text.getContent().getPOrTag().add(p);
            }
        }
        // remove empty
        for (int i = 0; i < text.getContent().getPOrTag().size(); i++) {
            if (text.getContent().getPOrTag().get(i) instanceof P) {
                p = (P) text.getContent().getPOrTag().get(i);
                for (int j = 0; j < p.getSe().size(); j++) {
                    s = p.getSe().get(j);
                    if (s.getWOrSOrZ().isEmpty()) {
                        p.getSe().remove(j);
                        j--;
                    }
                }
            }
        }

        try {
            // апошні параграф - толькі прагал што быў даданы ў канец тэксту
            P lastP = (P) text.getContent().getPOrTag().get(text.getContent().getPOrTag().size() - 1);
            if (lastP.getSe().size() == 1 && lastP.getSe().get(0).getWOrSOrZ().size() == 1
                    && lastP.getSe().get(0).getWOrSOrZ().get(0) instanceof S
                    && ((S) lastP.getSe().get(0).getWOrSOrZ().get(0)).getChar().equals(" ")) {
                text.getContent().getPOrTag().remove(text.getContent().getPOrTag().size() - 1);
            }
        } catch (Exception ex) {
        }

        return text;
    }
}
