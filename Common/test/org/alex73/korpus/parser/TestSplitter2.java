package org.alex73.korpus.parser;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.alex73.korpus.editor.core.GrammarDB;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.parser.Splitter2;
import org.alex73.korpus.text.xml.O;
import org.alex73.korpus.text.xml.P;
import org.alex73.korpus.text.xml.S;
import org.alex73.korpus.text.xml.W;
import org.alex73.korpus.text.xml.Z;
import org.junit.Before;
import org.junit.Test;

public class TestSplitter2 {
    P p;
    int se, w;

    IProcess errors = new IProcess() {
        
        @Override
        public void reportError(String error) {
        }

        @Override
        public void showStatus(String status) {
        }
    };

    @Before
    public void before() throws Exception {
        GrammarDB.initializeFromDir(new File("GrammarDB"), new GrammarDB.LoaderProgress() {
            public void setFilesCount(int count) {
            }

            public void beforeFileLoading(String file) {
            }

            public void afterFileLoading() {
            }
        });
    }

    @Test
    public void testText() {
        p = new Splitter2("Адно, слова... А: потым ? 123", true, errors).getP();

        nextW("Адно");
        nextZ(",");
        nextS(" ");
        nextW("слова");
        nextZ("\u2026");
        endSentence();

        nextS(" ");
        nextW("А");
        nextZ(":");
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

    private W nextW(String value) {
        Object obj = p.getSe().get(se).getWOrSOrZ().get(w);
        W result = (W) obj;
        assertEquals(value, result.getValue());
        w++;
        return result;
    }

    private W nextW(String value, String lemma, String tags) {
        Object obj = p.getSe().get(se).getWOrSOrZ().get(w);
        W result = (W) obj;
        assertEquals(value, result.getValue());
        w++;

        assertEquals(lemma, result.getLemma());
        Set<String> t1 = new TreeSet<>(Arrays.asList(tags.split("_")));
        Set<String> t2 = new TreeSet<>(Arrays.asList(result.getCat().split("_")));
        assertEquals(t1, t2);

        return result;
    }

    private S nextS(String value) {
        Object obj = p.getSe().get(se).getWOrSOrZ().get(w);
        S result = (S) obj;
        assertEquals(value, result.getChar());
        w++;
        return result;
    }

    private Z nextZ(String value) {
        Object obj = p.getSe().get(se).getWOrSOrZ().get(w);
        Z result = (Z) obj;
        assertEquals(value, result.getValue());
        w++;
        return result;
    }

    private O nextO(String value) {
        Object obj = p.getSe().get(se).getWOrSOrZ().get(w);
        O result = (O) obj;
        assertEquals(value, result.getValue());
        w++;
        return result;
    }

    private void endSentence() {
        assertEquals(p.getSe().get(se).getWOrSOrZ().size(), w);
        w = 0;
        se++;
    }

    private void endText() {
        assertEquals(p.getSe().size(), se);
    }

    @Test
    public void testGrammar() {
        p = new Splitter2("Беларусь п'еса", true, errors).getP();

        nextW("Беларусь", "Беларусь", "NPIINF3AS_NPIINF3NS");
        nextS(" ");
        nextW("п'еса", "п'е´са", "NCIINF2NS");
        endSentence();

        endText();
    }
}
