package org.alex73.korpus.parser;

import static org.junit.Assert.assertEquals;

import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.parser.PtextToKorpus;
import org.alex73.korpus.text.parser.Splitter3;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Word;
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
        p = PtextToKorpus.oneLine(new Splitter3(LanguageFactory.get("bel").getNormalizer(), true, errors).parse("- Адно, слова... А: потым ? 123 мо'' 'ак з'ява"));

        nextW("", "- ");
        nextW("Адно", ", ");
        nextW("слова", "...");
        endSentence();

        nextW("", " ");
        nextW("А", ": ");
        nextW("потым", " ?");
        endSentence();

        nextW("", " ");
        nextW("123", " ");
        nextW("мо", "'' '");
        nextW("ак", " ");
        nextW("з\u02BCява", "");
        endSentence();

        endText();
    }

    private void nextW(String word, String tail) {
        Word d = p.sentences[se].words[w];
        assertEquals(word, d.normalized);
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
