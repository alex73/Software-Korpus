package org.alex73.korpus.parser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import alex73.corpus.text.O;
import alex73.corpus.text.P;
import alex73.corpus.text.S;
import alex73.corpus.text.W;
import alex73.corpus.text.Z;

public class TestSplitter2 {
    P p;
    int se, w;

    @Test
    public void testText() {
        p = new Splitter2("Адно слова... А потым ? 123").getP();

        nextW("Адно");
        nextS(" ");
        nextW("слова");
        nextZ("\u2026");
        endSentence();

        nextS(" ");
        nextW("А");
        nextS(" ");
        nextW("потым");
        nextS(" ");
        nextZ("?");
        endSentence();

        nextS(" ");
        nextO("123");
        endSentence();

        endText();
    }

    private void nextW(String value) {
        Object obj = p.getSe().get(se).getWOrSOrZ().get(w);
        assertEquals(value, ((W) obj).getValue());
        w++;
    }

    private void nextS(String value) {
        Object obj = p.getSe().get(se).getWOrSOrZ().get(w);
        assertEquals(value, ((S) obj).getChar());
        w++;
    }

    private void nextZ(String value) {
        Object obj = p.getSe().get(se).getWOrSOrZ().get(w);
        assertEquals(value, ((Z) obj).getValue());
        w++;
    }

    private void nextO(String value) {
        Object obj = p.getSe().get(se).getWOrSOrZ().get(w);
        assertEquals(value, ((O) obj).getValue());
        w++;
    }

    private void endSentence() {
        assertEquals(p.getSe().get(se).getWOrSOrZ().size(), w);
        w = 0;
        se++;
    }

    private void endText() {
        assertEquals(p.getSe().size(), se);
    }
}
