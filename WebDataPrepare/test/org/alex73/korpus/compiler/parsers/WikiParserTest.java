package org.alex73.korpus.compiler.parsers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WikiParserTest {
    @Test
    public void wiki() {
        assertEquals("Main Page", WikiParser.fixText("[[Main Page]]"));
        assertEquals("different text", WikiParser.fixText("[[#See also|different text]]"));
        assertEquals("", WikiParser.fixText("[[Help:Contents]]"));

        assertEquals("call me", WikiParser.fixText("[skype:echo123 call me]"));
        assertEquals("", WikiParser.fixText("[https://mediawiki.org]"));
    }
}
