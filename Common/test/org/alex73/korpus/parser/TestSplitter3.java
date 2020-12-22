package org.alex73.korpus.parser;

import static org.junit.Assert.assertEquals;

import org.alex73.korpus.text.elements.Paragraph;
import org.alex73.korpus.text.elements.Word;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.parser.Splitter3;
import org.junit.Test;

public class TestSplitter3 {
    Paragraph p;
    int se, w;

    IProcess errors = new IProcess() {

        @Override
        public void reportError(String error, Throwable ex) {
        }

        @Override
        public void showStatus(String status) {
        }
    };

    @Test
    public void testText() {
        p = new Splitter3(true, errors).parse("- Адно, слова... А: потым ? 123 мо'' 'ак з'ява");

        nextW("", "- ");
        nextW("Адно", ", ");
        nextW("слова", "... ");
        endSentence();

        nextW("А", ": ");
        nextW("потым", " ? ");
        endSentence();

        nextW("123", " ");
        nextW("мо", "'' '");
        nextW("ак", " ");
        nextW("з'ява", "");
        endSentence();

        endText();
    }

    private void nextW(String word, String tail) {
        Word d = p.sentences[se].words[w];
        assertEquals(word, d.lightNormalized);
        assertEquals(tail, d.tail);
        w++;
    }

    private void endSentence() {
        assertEquals(p.sentences[se].words.length, w);
        w = 0;
        se++;
    }

    private void endText() {
        assertEquals(p.sentences.length, se);
    }
}
