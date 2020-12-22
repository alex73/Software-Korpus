package org.alex73.korpus.base;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class StaticGrammarFiller2Test {
    @Test
    public void testNorm() {
        StringBuilder list = new StringBuilder();
        StaticGrammarFiller2.add(list, "v1");
        assertEquals("v1", list.toString());
        StaticGrammarFiller2.add(list, "v1");
        assertEquals("v1", list.toString());
        StaticGrammarFiller2.add(list, "v2");
        assertEquals("v1;v2", list.toString());
        StaticGrammarFiller2.add(list, "v2");
        assertEquals("v1;v2", list.toString());
        StaticGrammarFiller2.add(list, "v3");
        assertEquals("v1;v2;v3", list.toString());
        StaticGrammarFiller2.add(list, "v3");
        assertEquals("v1;v2;v3", list.toString());
        StaticGrammarFiller2.add(list, "v1");
        StaticGrammarFiller2.add(list, "v2");
        StaticGrammarFiller2.add(list, "v3");
        assertEquals("v1;v2;v3", list.toString());
        StaticGrammarFiller2.add(list, "v");
        assertEquals("v1;v2;v3;v", list.toString());
        StaticGrammarFiller2.add(list, "2");
        assertEquals("v1;v2;v3;v;2", list.toString());
    }
}
