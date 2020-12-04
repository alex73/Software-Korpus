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

import org.alex73.korpus.base.BelarusianWordNormalizer;
import org.alex73.korpus.text.xml.ITextLineElement;
import org.alex73.korpus.text.xml.InlineTag;
import org.alex73.korpus.text.xml.O;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.S;
import org.alex73.korpus.text.xml.Se;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.Z;


/**
 * Гэты код дзеліць радок(ці некалькі радкоў для вершаў) на асобныя элемэнты XMLText.
 */
public class Splitter2 {
    private final IProcess errors;
    private final String para;
    private int pos;
    private char currentChar;
    private final P p = new P();
    private Se se;

    public Splitter2(String para, boolean processAmp, IProcess errors) {
        this.errors = errors;
        this.para = para;
        for (pos = 0; pos < para.length();) {
            currentChar = para.charAt(pos);

            if (currentChar == '&' && processAmp) {
                parseCharNameOrNumber();
                if (currentChar == 0) {
                    continue;
                }
            }

            switch (currentChar) {
            case '.':
            case '?':
            case '!':
                parseEndOfSentence();
                break;
            case '<':
                parseInlineTag();
                break;
            default:
                if (isSpace()) {
                    parseSpace();
                } else if (BelarusianWordNormalizer.isZnak(currentChar)) {
                    parseZnak();
                } else if (BelarusianWordNormalizer.isLetter(currentChar)) {
                    parseWord();
                } else {
                    parseOther(processAmp);
                }
                break;
            }
        }
    }

    public P getP() {
        return p;
    }

    public boolean isSentenceFinished() {
        return se == null;
    }

    /**
     * Char starts from '&'
     */
    private void parseCharNameOrNumber() {
        char next;
        try {
            next = para.charAt(pos + 1);
        } catch (StringIndexOutOfBoundsException ex) {
            return;
        }
        if (next == '#') {
            pos += 2;
        } else {
            pos++;
        }
        int end = pos;
        for (; end < para.length(); end++) {
            char ct = para.charAt(end);
            if (ct == ';') {
                break;
            }
        }

        String name = para.substring(pos, end);
        if (next == '#') { // char number
            currentChar = (char) Integer.parseInt(name);
            pos = end;
        } else { // char name
            switch (name) {
            case "nbsp":
                currentChar = ' ';
                break;
            case "oacute":
                currentChar = 'ó';
                break;
            case "quot":
                currentChar = '"';
                break;
            case "lt":
                currentChar = '<';
                break;
            case "gt":
                currentChar = '>';
                break;
            case "laquo":
                currentChar = '«';
                break;
            case "raquo":
                currentChar = '»';
                break;
            case "bdquo":
                currentChar = '„';
                break;
            case "ldquo":
                currentChar = '“';
                break;
            case "rdquo":
                currentChar = '”';
                break;
            case "ndash":
                currentChar = '–';
                break;
            case "mdash":
                currentChar = '—';
                break;
            case "rsquo":
                currentChar = '\u2019';
                break;
            case "shy":
                currentChar = 0; // soft hyphen
                break;
            default:
                errors.reportError("Error in amp: " + name);
                currentChar = ' ';
            }
        }
    }

    private void parseEndOfSentence() {
        while (true) {
            parseZnak();
            try {
                currentChar = para.charAt(pos);
            } catch (StringIndexOutOfBoundsException ex) {
                currentChar = 0;
            }
            if (currentChar != '.' && currentChar != '!' && currentChar != '?') {
                break;
            }
        }
        se = null;
    }

    private void parseSpace() {
        S s = new S();
        s.setChar(Character.toString(currentChar));
        addToSentence(s);
        pos++;
    }

    private void parseZnak() {
        boolean dots3 = false;
        try {// 3 dots
            dots3 = currentChar == '.' && para.charAt(pos + 1) == '.' && para.charAt(pos + 2) == '.';
        } catch (StringIndexOutOfBoundsException ex) {
        }
        try {// after 3 dots
            if (para.charAt(pos + 3) == '.') {
                dots3 = false;
            }
        } catch (StringIndexOutOfBoundsException ex) {
        }
        if (dots3) {
            pos += 3;
            currentChar = '\u2026';
        } else {
            pos++;
        }
        Z z = new Z(currentChar);
        addToSentence(z);
    }

    private void parseInlineTag() {
        int end = pos;
        for (; end < para.length(); end++) {
            currentChar = para.charAt(end);
            if (currentChar == '>') {
                end++;
                break;
            }
        }
        InlineTag w = new InlineTag();
        w.setValue(para.substring(pos, end));
        pos = end;
        addToSentence(w);
    }

    private void parseWord() {
        int end = pos;
        for (; end < para.length(); end++) {
            currentChar = para.charAt(end);
            if (!BelarusianWordNormalizer.isLetter(currentChar)) {
                break;
            }
        }
        W w = new W();
        w.setValue(para.substring(pos, end));
        pos = end;
        addToSentence(w);
    }

    private void parseOther(boolean processAmp) {
        int end = pos;
        for (; end < para.length(); end++) {
            currentChar = para.charAt(end);
            if (BelarusianWordNormalizer.isLetter(currentChar) || BelarusianWordNormalizer.isZnak(currentChar) || isSpace() || currentChar == '<') {
                break;
            }
            if (processAmp && currentChar == '&') {
                break;
            }
        }

        O o = new O();
        o.setValue(para.substring(pos, end));
        pos = end;
        addToSentence(o);
    }

    private boolean isSpace() {
        return Character.isWhitespace(currentChar);
    }

    private void addToSentence(ITextLineElement obj) {
        if (se == null) {
            se = new Se();
            p.getSe().add(se);
        }
        se.getWOrSOrZ().add(obj);
    }
}
