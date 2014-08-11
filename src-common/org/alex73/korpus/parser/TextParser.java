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

import org.alex73.korpus.editor.core.structure.KorpusDocument;
import org.alex73.korpus.editor.core.structure.Line;
import org.apache.commons.lang.StringUtils;

import alex73.corpus.paradigm.TEI;
import alex73.corpus.paradigm.TeiHeader;

/**
 * Чытаньне тэкставага файлу й стварэньне дакумэнту корпуса.
 */
public class TextParser {
    protected static JAXBContext CONTEXT;
    static {
        try {
            CONTEXT = JAXBContext.newInstance(TEI.class.getPackage().getName());
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static KorpusDocument parseText(InputStream in, boolean headerOnly) throws Exception {
        BufferedReader rd = new BOMBufferedReader(new InputStreamReader(in, "UTF-8"));

        Map<String, String> headers = extractHeaders(rd);

        KorpusDocument doc = new KorpusDocument();
        String a = headers.get("Authors");
        doc.textInfo.authors = a != null ? a.split(",") : new String[0];
        doc.textInfo.title = headers.get("Title");
        doc.textInfo.styleGenre = headers.get("StyleGenre");
        doc.textInfo.writtenYear = parseDate(headers.get("YearWritten"));
        doc.textInfo.publishedYear = parseDate(headers.get("YearPublished"));

        if (doc.textInfo.authors.length == 0) {
            throw new Exception("Нявызначаны аўтар");
        }
        if (StringUtils.isBlank(doc.textInfo.title)) {
            throw new Exception("Нявызначаная назва");
        }
        if (headerOnly) {
            return doc;
        }

        String s;
        while ((s = rd.readLine()) != null) {
            Splitter sp = new Splitter(s);
            doc.add(sp.splitParagraph());
        }

        return doc;
    }
    
    static final Pattern RE_DATE1=Pattern.compile("([0-9]{4})");
    public static Integer parseDate(String date) {
        if (date==null) {
            return null;
        }
        
        Matcher m;
        if ((m=RE_DATE1.matcher(date)).matches()) {
            return Integer.parseInt(m.group(1));
        }else {
            throw new RuntimeException("Wrong date: "+date);
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

    static Pattern RE_HEADER = Pattern.compile("##([A-Za-z0-9]+):\\s*(.*)");

    static Map<String, String> extractHeaders(BufferedReader rd) throws Exception {
        Map<String, String> result = new TreeMap<String, String>();

        String s;
        while ((s = rd.readLine()) != null) {
            if (s.trim().length() == 0) {
                break;
            }
            Matcher m = RE_HEADER.matcher(s);
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
            Matcher m = RE_HEADER.matcher(s);
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

    static TeiHeader createTeiHeader(Map<String, String> headers) {
        TeiHeader tei = new TeiHeader();

        return tei;
    }
}
