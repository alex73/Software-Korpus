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

package org.alex73.korpus.editor.core.doc.structure;

import java.util.Locale;
import java.util.regex.Pattern;

import org.alex73.korpus.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.editor.MainController;

/**
 * Гэты код дзеліць радок на асобныя элемэнты.
 */
public class LineSplitter {

    public static final Locale BEL = new Locale("be");

    public static final char CH_3DOTS = '\u2026';
    public static final char CH_SENT_SEPARATOR = '�';

    protected static final Pattern RE_SPLIT_W = Pattern.compile("\\s+");
    protected static final Pattern RE_AMP = Pattern.compile("&(#([0-9]+)|([a-z]+));");

    enum SPLIT_MODE {
        WORD, SPACE, TAG_SHORT
    };

    String line;
    Line result = new Line();
    int partStart = 0;
    int currentPos;
    SPLIT_MODE mode	;

    public LineSplitter(String line) {
        this.line = line;
    }

    private static StringBuilder str = new StringBuilder();
    private static StringBuilder temp = new StringBuilder();

   /* public static synchronized String presplit(String line) {
        str.setLength(0);
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            char prev, next;
            try {
                prev = line.charAt(i - 1);
            } catch (StringIndexOutOfBoundsException ex) {
                prev = 0;
            }
            try {
                next = line.charAt(i + 1);
            } catch (StringIndexOutOfBoundsException ex) {
                next = 0;
            }
            try {// 3 dots
                if (ch == '.' && next == '.' && line.charAt(i + 2) == '.' && line.charAt(i + 3) != '.') {
                    ch = CH_3DOTS;
                    i += 2;
                }
            } catch (StringIndexOutOfBoundsException ex) {
            }
            if (ch == '&') {
                temp.setLength(0);
                if (next == '#') {
                    i += 2;
                } else {
                    i++;
                }
                for (; i < line.length(); i++) {
                    char ct = line.charAt(i);
                    if (ct == ';') {
                        break;
                    }
                    temp.append(ct);
                }
                if (next == '#') { // char number
                    ch = (char) Integer.parseInt(temp.toString());
                } else { // char name
                    switch (temp.toString()) {
                    case "nbsp":
                        ch = ' ';
                        break;
                    case "oacute":
                        ch = 'ó';
                        break;
                    case "quot":
                        ch = '"';
                        break;
                    case "lt":
                        ch = '<';
                        break;
                    case "gt":
                        ch = '>';
                        break;
                    case "laquo":
                        ch = '«';
                        break;
                    case "raquo":
                        ch = '»';
                        break;
                    case "bdquo":
                        ch = '„';
                        break;
                    case "ldquo":
                        ch = '“';
                        break;
                    case "rdquo":
                        ch = '”';
                        break;
                    case "ndash":
                        ch = '–';
                        break;
                    case "mdash":
                        ch = '—';
                        break;
                    default:
                        System.err.println("Error in amp: " + line);
                        ch = ' ';
                    }
                }
            }
            switch (ch) {
            case '\u00A0':
            case '\t':
                ch = ' ';
                break;
            case '’':
            case '`':
            case '´':
            case '‘':
                ch = '\'';
                break;
            }
            str.append(ch);
            if (ch == '.' || ch == '?' || ch == '!' || ch == CH_3DOTS) {
                str.append(CH_SENT_SEPARATOR);
            }
        }
        if (str.length() > 0 && str.charAt(str.length() - 1) == CH_SENT_SEPARATOR) {
            str.setLength(str.length() - 1);
        }

        return str.toString();
    }*/

    void flush() {
        if (currentPos > partStart) {
            String part = line.substring(partStart, currentPos);
            switch (mode) {
            case WORD:
                WordItem w=new WordItem(part);
                MainController.gr.filler.fillNonManual(w);
                result.add(w);
                break;
            case SPACE:
                result.add(new TailItem(part));
                break;
            case TAG_SHORT:
                result.add(new InlineTag(part));
                break;
            }
            partStart = currentPos;
        }
    }

    public Line splitParagraph() {
        mode = SPLIT_MODE.SPACE;
        for (currentPos = 0; currentPos < line.length(); currentPos++) {
            char ch = line.charAt(currentPos);
            if (ch == CH_SENT_SEPARATOR) {
                flush();
                result.add(new SentenceSeparatorItem());
                partStart = currentPos + 1;
                mode = SPLIT_MODE.SPACE;
                continue;
            }
            switch (mode) {
            case WORD:
                if (ch == '<') {
                    flush();
                    partStart = currentPos;
                    mode = SPLIT_MODE.TAG_SHORT;
                } else if (BelarusianWordNormalizer.isLetter(ch)) {
                } else {
                    flush();
                    if (ch == ' ') {
                        result.add(new TailItem(" "));
                    } else {
                        result.add(new TailItem(line.substring(currentPos, currentPos + 1)));
                    }
                    partStart = currentPos + 1;
                    mode = SPLIT_MODE.SPACE;
                }
                break;
            case SPACE:
                if (ch == '<') {
                    flush();
                    partStart = currentPos;
                    mode = SPLIT_MODE.TAG_SHORT;
                } else if (BelarusianWordNormalizer.isLetter(ch)) {
                    flush();
                    partStart = currentPos;
                    mode = SPLIT_MODE.WORD;
                } else {
                    flush();
                    if (ch == ' ') {
                        result.add(new TailItem(" "));
                    } else {
                        result.add(new TailItem(line.substring(currentPos, currentPos + 1)));
                    }
                    partStart = currentPos + 1;
                    mode = SPLIT_MODE.SPACE;
                }
                break;
            case TAG_SHORT:
                if (ch == '>') {
                    currentPos++;
                    flush();
                    currentPos--;
                    partStart = currentPos + 1;
                    mode = SPLIT_MODE.SPACE;
                }
                break;
            default:
                throw new RuntimeException();
            }
        }
        flush();

        result.normalize();
        return result;
    }

    /*public static Object splitChar(char ch) {
        if (ch == CH_SENT_SEPARATOR) {
            return new SentenceSeparatorItem();
        } else if (GrammarDB.getInstance().getZnaki().indexOf(ch) >= 0) {
            return new Z(ch);
        } else if (Character.isWhitespace(ch)) {
            return new S(ch);
        } else {
            return GrammarFiller.getWordInfo("" + ch);
        }
    }*/



   /* protected static String middle(String v) {
        if (v == null) {
            return null;
        }
        return v.substring(1, v.length() - 1);
    }*/

    /*protected static void punctuations(List<String> words) {
        String punctChars = GrammarDB.getInstance().getZnaki();
        for (int i = 0; i < words.size(); i++) {
            String w = words.get(i);
            for (int j = 0; j < w.length(); j++) {
                if (punctChars.indexOf(w.charAt(j)) >= 0) {
                    // punctuation char
                    String wbefore = w.substring(0, j);
                    String wpunct = w.substring(j, j + 1);
                    String wafter = w.substring(j + 1);

                    if (wbefore.length() > 0 || wafter.length() > 0) {
                        words.remove(i);
                        if (wafter.length() > 0) {
                            words.add(i, wafter);
                        }
                        words.add(i, wpunct);
                        if (wbefore.length() > 0) {
                            words.add(i, wbefore);
                        }
                        break;
                    }
                }
            }
        }
    }*/
}
