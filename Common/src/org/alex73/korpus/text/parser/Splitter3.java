package org.alex73.korpus.text.parser;

import java.util.ArrayList;
import java.util.List;

import org.alex73.korpus.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.text.elements.Paragraph;
import org.alex73.korpus.text.elements.Sentence;
import org.alex73.korpus.text.elements.Word;

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
    private final List<Sentence> currentParagraph = new ArrayList<>();
    private final List<Word> currentSentence = new ArrayList<>();
    private final StringBuilder currentWord = new StringBuilder();
    private final StringBuilder currentTail = new StringBuilder();

    public Splitter3(boolean processAmp, IProcess errors) {
        this.processAmp = processAmp;
        this.errors = errors;
    }

    public Paragraph parse(CharSequence para) {
        this.para = para;
        currentParagraph.clear();
        currentSentence.clear();
        currentWord.setLength(0);
        currentTail.setLength(0);
        for (pos = 0; pos < para.length(); pos++) {
            currentChar = para.charAt(pos);
            switch (currentChar) {
            case '.':
            case '?':
            case '!':
                appendZnak();
                closeWord();
                closeSentence();
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

        Paragraph p = new Paragraph();
        p.sentences = currentParagraph.toArray(new Sentence[0]);
        return p;
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
        if (currentWord.length() == 0 && currentTail.length() == 0) {
            return;
        }
        if (currentWord.length() == 0 && currentTail.length() > 0) {
            // append to previous tail ?
            if (!currentSentence.isEmpty()) {
                throw new RuntimeException("Shouldn't be");
            }
            if (!currentParagraph.isEmpty()) {
                Sentence latestSentence = currentParagraph.get(currentParagraph.size() - 1);
                if (latestSentence.words.length > 0) {
                    Word latestWord = latestSentence.words[latestSentence.words.length - 1];
                    latestWord.tail += currentTail;
                    currentTail.setLength(0);
                    return;
                } else {
                    throw new RuntimeException("Shouldn't be");
                }
            }
        }
        Word w = new Word();
        w.lightNormalized = BelarusianWordNormalizer.lightNormalized(currentWord);
        w.tail = currentTail.toString();
        currentSentence.add(w);
        currentWord.setLength(0);
        currentTail.setLength(0);
    }

    private void closeSentence() {
        if (currentSentence.isEmpty()) {
            return;
        }
        Sentence se = new Sentence();
        se.words = currentSentence.toArray(new Word[0]);
        currentParagraph.add(se);
        currentSentence.clear();
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
