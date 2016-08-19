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
 * 
 * @deprecated Use TextGeneral
 */
@Deprecated
public class TextParser {

   /* public static XMLText parseText(InputStream in, boolean headerOnly, IProcess errors) throws Exception {
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
    }*/

    

}
