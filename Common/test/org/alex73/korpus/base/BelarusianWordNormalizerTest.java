package org.alex73.korpus.base;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BelarusianWordNormalizerTest {
    @Test
    public void testNorm() {
        assertEquals("Ко+", BelarusianWordNormalizer.normalizePreserveCase("Ко+"));
        assertEquals("ко+", BelarusianWordNormalizer.normalizeLowerCase("Ко+"));
        assertEquals("Ко", BelarusianWordNormalizer.normalizePreserveCase("Ко"));
        assertEquals("ко", BelarusianWordNormalizer.normalizeLowerCase("Ко"));
    }
}
