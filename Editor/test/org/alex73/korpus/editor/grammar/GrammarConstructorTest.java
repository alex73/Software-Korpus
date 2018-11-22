package org.alex73.korpus.editor.grammar;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GrammarConstructorTest {
    @Test
    public void testCompareEnds() {
        assertEquals(11, GrammarConstructor.compareEnds("абаку+львацца", "пераку+львацца"));
        assertEquals(10, GrammarConstructor.compareEnds("абакульвацца", "пераку+львацца"));
        assertEquals(3, GrammarConstructor.compareEnds("абакульва+цца", "пераку+львацца"));
    }

    @Test
    public void testConstructWords() {
        assertEquals("абаку+львацца",
                GrammarConstructor.constructWord("абаку+львацца", "пераку+львацца", 11, "пераку+львацца"));
        assertEquals("абаку+львае",
                GrammarConstructor.constructWord("абаку+львацца", "пераку+львацца", 11, "пераку+львае"));

        assertEquals("абаку+львацца",
                GrammarConstructor.constructWord("абакульвацца", "пераку+львацца", 10, "пераку+львацца"));
        assertEquals("абаку+львае",
                GrammarConstructor.constructWord("абакульвацца", "пераку+львацца", 10, "пераку+львае"));

        assertEquals("абакульва+е",
                GrammarConstructor.constructWord("абаку+львацца", "пераку+львацца", 11, "перакульва+е"));
    }
}
