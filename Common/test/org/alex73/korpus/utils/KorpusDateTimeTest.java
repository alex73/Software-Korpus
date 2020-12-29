package org.alex73.korpus.utils;

import org.junit.Test;

public class KorpusDateTimeTest {
    @Test
    public void testXmlDateTime() {
        new KorpusDateTime("2015-03-17");
        new KorpusDateTime("2015-03-17T16:30:38");
        new KorpusDateTime("2015-03-17T16:30:38+00:00");
        new KorpusDateTime("07.05.1994-31.08.1996");
        new KorpusDateTime("2015?");
        new KorpusDateTime("[2015]");
    }
}
