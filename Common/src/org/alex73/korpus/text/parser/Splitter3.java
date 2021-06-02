package org.alex73.korpus.text.parser;

import org.alex73.korpus.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.text.structure.files.ITextLineElement;
import org.alex73.korpus.text.structure.files.SentenceSeparatorItem;
import org.alex73.korpus.text.structure.files.TailItem;
import org.alex73.korpus.text.structure.files.TextLine;
import org.alex73.korpus.text.structure.files.WordItem;

/**
 * Гэты код дзеліць радок(ці некалькі радкоў для вершаў) на асобныя сказы і
 * словы.
 */
public class Splitter3 {
    private final IProcess errors;
    private final boolean processAmp;
    private CharSequence para;
    private int pos;
    private char currentChar;
    private TextLine result;
    private final StringBuilder currentWord = new StringBuilder();
    private final StringBuilder currentTail = new StringBuilder();

    public Splitter3(boolean processAmp, IProcess errors) {
        this.processAmp = processAmp;
        this.errors = errors;
    }

    public TextLine parse(CharSequence para) {
        this.para = para;
        result = new TextLine();
        currentWord.setLength(0);
        currentTail.setLength(0);
        for (pos = 0; pos < para.length(); pos++) {
            currentChar = para.charAt(pos);
            switch (currentChar) {
            case '.':
            case '?':
            case '!':
                if (!result.isEmpty() && (result.get(result.size() - 1) instanceof SentenceSeparatorItem)) {
                    ITextLineElement prev = result.get(result.size() - 2);
                    TailItem tail = (TailItem) prev;
                    tail.text += currentChar;
                } else {
                    appendZnak();
                    closeWord();
                    closeSentence();
                }
                break;
            case '<':
                parseInlineTag();
                break;
            default:
                if (currentChar == '&' && processAmp) {
                    parseCharNameOrNumber();
                    if (currentChar == 0) {
                        continue;
                    }
                }
                if (BelarusianWordNormalizer.apostrafy.indexOf(currentChar) >= 0 || currentChar == '-') {
                    // не могуць быць на мяжы
                    if (currentWord.length() > 0 && currentTail.length() == 0) {
                        currentWord.append(currentChar);
                    } else {
                        appendZnak();
                    }
                    continue;
                }
                if (BelarusianWordNormalizer.isLetter(currentChar)) {
                    if (currentTail.length() > 0) {
                        closeWord();
                    }
                    currentWord.append(currentChar);
                } else {
                    appendZnak();
                }
                break;
            }
        }
        closeWord();
        closeSentence();

        return result;
    }

    private void appendZnak() {
        if (currentTail.length() == 0) {
            // check if word ends with znak
            while (currentWord.length() > 0) {
                char latestInWord = currentWord.charAt(currentWord.length() - 1);
                if (BelarusianWordNormalizer.apostrafy.indexOf(latestInWord) >= 0 || latestInWord == '-') {
                    currentWord.setLength(currentWord.length() - 1);
                    currentTail.insert(0, latestInWord);
                } else {
                    break;
                }
            }
        }
        currentTail.append(currentChar);
    }

    private void closeWord() {
        if (currentWord.length() > 0) {
            WordItem w = new WordItem();
            w.lightNormalized = BelarusianWordNormalizer.lightNormalized(currentWord);
            result.add(w);
        }
        if (currentTail.length() > 0) {
            TailItem t = new TailItem(currentTail.toString());
            result.add(t);
        }
        currentWord.setLength(0);
        currentTail.setLength(0);
    }

    private void closeSentence() {
        result.add(new SentenceSeparatorItem());
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

        String name = para.subSequence(pos, end).toString();
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
            case "amp":
                currentChar = '&'; // soft hyphen
                break;
            default:
                errors.reportError("Error in amp: " + name, null);
                currentChar = ' ';
            }
        }
    }

    private void parseInlineTag() {
        while (pos < para.length()) {
            if (para.charAt(pos) == '>') {
                break;
            }
            pos++;
        }
    }
}
