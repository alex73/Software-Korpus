package org.alex73.fanetyka.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.alex73.grammardb.GrammarDB2;
import org.alex73.grammardb.GrammarFinder;
import org.junit.Test;

public class Fanetyka3Test {
    @Test
    public void testWordCompare() throws Exception {
        assertTrue(Fanetyka3.compareWord(Fanetyka3.normalize("Снег"), "снег"));
        assertFalse(Fanetyka3.compareWord(Fanetyka3.normalize("снег"), "Снег"));
        assertTrue(Fanetyka3.compareWord(Fanetyka3.normalize("Снег"), "Снег"));
        assertFalse(Fanetyka3.compareWord(Fanetyka3.normalize("снег"), "сьнег"));
        assertTrue(Fanetyka3.compareWord(Fanetyka3.normalize("ўнёс"), "унёс"));
        assertFalse(Fanetyka3.compareWord(Fanetyka3.normalize("унёс"), "ўнёс"));
        assertTrue(Fanetyka3.compareWord(Fanetyka3.normalize("ганак"), "ґанак"));
        assertFalse(Fanetyka3.compareWord(Fanetyka3.normalize("ґанак"), "ганак"));
        assertTrue(Fanetyka3.compareWord(Fanetyka3.normalize("ганак"), "га+нак"));
        assertTrue(Fanetyka3.compareWord(Fanetyka3.normalize("га´нак"), "ганак"));

        GrammarDB2 gr = GrammarDB2.empty();
        GrammarFinder gf = new GrammarFinder(gr);
        assertEquals("suˈrjɔzna suˈrjɔzna", new FanetykaText(gf, "сурʼёзна сур'ёзна").ipa);
    }
}
