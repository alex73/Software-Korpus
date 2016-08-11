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

import java.util.Set;
import java.util.TreeSet;

import org.alex73.korpus.editor.core.GrammarDB;
import org.alex73.korpus.text.xml.ITextLineElement;
import org.alex73.korpus.text.xml.InlineTag;
import org.alex73.korpus.text.xml.O;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.S;
import org.alex73.korpus.text.xml.Se;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.Z;
import org.alex73.korpus.utils.SetUtils;
import org.alex73.korpus.utils.WordNormalizer;

import alex73.corpus.paradigm.Form;
import alex73.corpus.paradigm.Paradigm;
import alex73.corpus.paradigm.Variant;

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
                } else if (isZnak()) {
                    parseZnak();
                } else if (isLetter()) {
                    parseWord();
                } else {
                    parseOther(processAmp);
                }
                break;
            }
        }

        fillInfo();
    }

    private void fillInfo() {
        for (int i = 0; i < p.getSe().size(); i++) {
            Se se = p.getSe().get(i);
            for (int j = 0; j < se.getWOrSOrZ().size(); j++) {
                Object o = se.getWOrSOrZ().get(j);
                if (o instanceof W) {
                    W w = (W) o;
                    String word = fixWord(w.getValue());
                    Paradigm[] paradigms = GrammarDB.getInstance().getParadigmsByForm(word);
                    fillWordInfoParadigms(w, word, paradigms);
                } else if (o instanceof S) {
                } else if (o instanceof Z) {
                    Z z=(Z)o;
                    Paradigm[] paradigms = GrammarDB.getInstance().getParadigmsByForm(z.getText());
                    fillZnakInfoParadigms(z, paradigms);
                } else if (o instanceof O) {
                } else if (o instanceof InlineTag) {
                } else {
                    throw new RuntimeException("Unknown type:" + o.getClass());
                }
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
        Z z = new Z();
        z.setValue(Character.toString(currentChar));
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
            if (!isLetter()) {
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
            if (isLetter() || isZnak() || isSpace() || currentChar == '<') {
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

    private boolean isZnak() {
        String znaki = GrammarDB.getInstance().getZnaki();
        return znaki.indexOf(currentChar) >= 0;
    }

    private boolean isLetter() {
        String letters = GrammarDB.getInstance().getLetters();
        return letters.indexOf(currentChar) >= 0;
    }

    private void addToSentence(ITextLineElement obj) {
        if (se == null) {
            se = new Se();
            p.getSe().add(se);
        }
        se.getWOrSOrZ().add(obj);
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
        if (paradigms != null) {
            for (Paradigm p : paradigms) {
                lemmas.add(p.getLemma());
                boolean foundForm = false;
                for(Variant v:p.getVariant()) {
                for (Form f : v.getForm()) {
                    if (word.equals(f.getValue())) {
                        cats.add(p.getTag() + f.getTag());
                        foundForm = true;
                    }
                }}
                if (!foundForm) {
                    // the same find, but without stress and lowercase
                    String uw = WordNormalizer.normalize(word);
                    for(Variant v:p.getVariant()) {
                    for (Form f : v.getForm()) {
                        if (uw.equals(WordNormalizer.normalize(f.getValue()))) {
                            cats.add(p.getTag() + f.getTag());
                        }
                    }}
                }
            }
        }
        w.setLemma(SetUtils.set2string(lemmas));
        w.setCat(SetUtils.set2string(cats));
    }

    static void fillZnakInfoParadigms(Z z, Paradigm[] paradigms) {
        Set<String> cats = new TreeSet<>();
        for (Paradigm p : paradigms) {
            for(Variant v:p.getVariant()) {
            for (Form f : v.getForm()) {
                if (z.getText().equals(f.getValue())) {
                    cats.add(p.getTag() + f.getTag());
                }
            }
        }}
        z.setCat(SetUtils.set2string(cats));
    }
}
