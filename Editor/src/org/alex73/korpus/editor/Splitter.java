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

package org.alex73.korpus.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.alex73.korpus.editor.core.GrammarDB;
import org.alex73.korpus.editor.core.structure.Line;
import org.alex73.korpus.editor.core.structure.SentenceSeparatorItem;
import org.alex73.korpus.text.xml.InlineTag;
import org.alex73.korpus.text.xml.S;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.Z;
import org.alex73.korpus.utils.WordNormalizer;

import alex73.corpus.paradigm.Paradigm;

/**
 * Гэты код дзеліць радок на асобныя элемэнты.
 */
public class Splitter {

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
    SPLIT_MODE mode;

    public Splitter(String line) {
        this.line = line;
    }

    private static StringBuilder str = new StringBuilder();
    private static StringBuilder temp = new StringBuilder();

    public static synchronized String presplit(String line) {
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
    }

    void flush() {
        if (currentPos > partStart) {
            String part = line.substring(partStart, currentPos);
            switch (mode) {
            case WORD:
                result.add(getWordInfo(part));
                break;
            case SPACE:
                result.add(new S(part));
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
                } else if (GrammarDB.getInstance().getLetters().indexOf(ch) >= 0) {
                } else {
                    flush();
                    if (ch == ' ') {
                        S s = new S();
                        s.setChar(" ");
                        result.add(s);
                    } else {
                        result.add(getZnakInfo(line.substring(currentPos, currentPos + 1)));
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
                } else if (GrammarDB.getInstance().getLetters().indexOf(ch) >= 0) {
                    flush();
                    partStart = currentPos;
                    mode = SPLIT_MODE.WORD;
                } else {
                    flush();
                    if (ch == ' ') {
                        result.add(new S(' '));
                    } else {
                        result.add(getZnakInfo(line.substring(currentPos, currentPos + 1)));
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

    public static Object splitChar(char ch) {
        if (ch == CH_SENT_SEPARATOR) {
            return new SentenceSeparatorItem();
        } else if (GrammarDB.getInstance().getZnaki().indexOf(ch) >= 0) {
            return getZnakInfo("" + ch);
        } else if (Character.isWhitespace(ch)) {
            return new S(ch);
        } else {
            return getWordInfo("" + ch);
        }
    }

    protected static W getWordInfo(String w) {
        String word = fixWord(w);
        W result = new W(w); // value must be original text
        Paradigm[] paradigms = GrammarDB.getInstance().getParadigmsByForm(word);
        if (paradigms != null) {
            fillWordInfoParadigms(result, word, paradigms);
        }
        return result;
    }

    public static Z getZnakInfo(String w) {
        String word = fixWord(w);
        Z result = new Z();
        result.setValue(w); // value must be original text
        Paradigm[] paradigms = GrammarDB.getInstance().getParadigmsByForm(word);
        if (paradigms != null) {
            fillZnakInfoParadigms(result, word, paradigms);
        }
        return result;
    }

    public static void fillWordsInfo(Line line) {
        for (Object item : line) {
            if (item instanceof W) {
                W w = (W) item;
                if (w.getLemma() == null) {
                    String word = w.getValue();
                    Paradigm[] paradigms = GrammarDB.getInstance().getParadigmsByForm(word);
                    if (paradigms != null) {
                        fillWordInfoParadigms(w, word, paradigms);
                    }
                }
            } else if (item instanceof Z) {
                Z z = (Z) item;
                String word = z.getValue(); // TODO : check
                Paradigm[] paradigms = GrammarDB.getInstance().getParadigmsByForm(word);
                if (paradigms != null) {
                    fillZnakInfoParadigms(z, word, paradigms);
                }
            }
        }
    }

    public static void fillWordInfoPagadigm(W w, Paradigm paradygm) {
        String word = fixWord(w.getValue());

        Paradigm[] paradigms = new Paradigm[1];
        paradigms[0] = paradygm;
        w.setLemma(null);
        w.setCat(null);
        fillWordInfoParadigms(w, word.toLowerCase(BEL), paradigms);
    }

    public static void fillWordInfoLemma(W w, String lemma) {
        String word = fixWord(w.getValue());

        Paradigm[] paradigms = GrammarDB.getInstance().getParadigmsByForm(word);
        if (paradigms == null) {
            return;
        }
        List<Paradigm> pt = new ArrayList<>(paradigms.length);
        for (Paradigm p : paradigms) {
            if (p.getLemma().equals(lemma)) {
                pt.add(p);
            }
        }
        fillWordInfoParadigms(w, word, pt.toArray(new Paradigm[pt.size()]));
    }

    public static String fixWord(String word) {
        // Ў
        if (word.startsWith("ў")) {
            word = "у" + word.substring(1);
        } else if (word.startsWith("Ў")) {
            word = "У" + word.substring(1);
        }
        return word;
    }

    static void fillWordInfoParadigms(W w, String word, Paradigm[] paradigms) {
        Set<String> lemmas = new TreeSet<>();
        Set<String> cats = new TreeSet<>();
        for (Paradigm p : paradigms) {
            lemmas.add(p.getLemma());
            boolean foundForm = false;
            for (Paradigm.Form f : p.getForm()) {
                if (word.equals(f.getValue())) {
                    cats.add(p.getTag() + f.getTag());
                    foundForm = true;
                }
            }
            if (!foundForm) {
                // the same find, but without stress and lowercase
                String uw = WordNormalizer.normalize(word);
                for (Paradigm.Form f : p.getForm()) {
                    if (uw.equals(WordNormalizer.normalize(f.getValue()))) {
                        cats.add(p.getTag() + f.getTag());
                    }
                }
            }
        }
        w.setLemma(set2string(lemmas));
        w.setCat(set2string(cats));
    }

    static void fillZnakInfoParadigms(Z z, String word, Paradigm[] paradigms) {
        Set<String> cats = new TreeSet<>();
        for (Paradigm p : paradigms) {
            boolean foundForm = false;
            for (Paradigm.Form f : p.getForm()) {
                if (word.equals(f.getValue())) {
                    cats.add(p.getTag() + f.getTag());
                    foundForm = true;
                }
            }
            if (!foundForm) {
                // the same find, but without stress and lowercase
                String uw = WordNormalizer.normalize(word);
                for (Paradigm.Form f : p.getForm()) {
                    if (uw.equals(WordNormalizer.normalize(f.getValue()))) {
                        cats.add(p.getTag() + f.getTag());
                    }
                }
            }
        }
        z.setCat(set2string(cats));
    }

    protected static String set2string(Set<String> set) {
        if (set.isEmpty()) {
            return null;
        }
        StringBuilder r = new StringBuilder();
        for (String s : set) {
            if (r.length() > 0) {
                r.append('_');
            }
            r.append(s);
        }
        return r.toString();
    }

    protected static String middle(String v) {
        if (v == null) {
            return null;
        }
        return v.substring(1, v.length() - 1);
    }

    protected static void punctuations(List<String> words) {
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
    }
}