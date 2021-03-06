package org.alex73.korpus.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.alex73.korpus.belarusian.BelarusianWordNormalizer;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class BelarusianWordNormalizerTest {
    @Test
    public void testHash() {
        assertEquals(BelarusianWordNormalizer.hash("Ко+"), BelarusianWordNormalizer.hash("ко"));
        assertEquals(BelarusianWordNormalizer.hash("Кў"), BelarusianWordNormalizer.hash("ку"));
        assertEquals(BelarusianWordNormalizer.hash("яг"), BelarusianWordNormalizer.hash("ЯҐ"));
    }

    @Test
    public void testEquals() {
        assertTrue(BelarusianWordNormalizer.equals("Ко+", "Ко+"));
        assertTrue(BelarusianWordNormalizer.equals("ко+", "Ко+"));
        assertTrue(BelarusianWordNormalizer.equals("Ко", "Ко"));
        assertTrue(BelarusianWordNormalizer.equals("Ко", "Ко"));
        assertTrue(BelarusianWordNormalizer.equals("Менск", "Менск"));
        assertTrue(BelarusianWordNormalizer.equals("шмат", "Шмат"));
        assertTrue(BelarusianWordNormalizer.equals("уэлс", "Ўэлс"));
        assertTrue(BelarusianWordNormalizer.equals("уэлс", "ўэлс"));
        assertTrue(BelarusianWordNormalizer.equals("Ўэлс", "Ўэлс"));
        assertFalse(BelarusianWordNormalizer.equals("Ўэлс", "ўэлс"));
        assertFalse(BelarusianWordNormalizer.equals("Ўэлс", "Уэлс"));
        assertFalse(BelarusianWordNormalizer.equals("шмат", "шматок"));
        assertFalse(BelarusianWordNormalizer.equals("шмат", "шматы"));
        assertFalse(BelarusianWordNormalizer.equals("шматы", "шмат"));
        assertFalse(BelarusianWordNormalizer.equals("шматок", "шмат"));
        assertFalse(BelarusianWordNormalizer.equals("шмат", "няшмат"));
        assertFalse(BelarusianWordNormalizer.equals("шмат", "шамк"));
        assertTrue(BelarusianWordNormalizer.equals("малы+", "малы+"));
        assertTrue(BelarusianWordNormalizer.equals("малы+", "малы"));
        assertFalse(BelarusianWordNormalizer.equals("малы+", "ма+лы"));
        assertTrue(BelarusianWordNormalizer.equals("малы´", "малы"));
        assertTrue(BelarusianWordNormalizer.equals("малы´", "малы´"));
        assertFalse(BelarusianWordNormalizer.equals("малы´", "ма´лы"));
        assertTrue(BelarusianWordNormalizer.equals("малы", "ма+лы"));
        assertTrue(BelarusianWordNormalizer.equals("малы", "ма+лы+"));
        assertTrue(BelarusianWordNormalizer.equals("Нью-Ё+рк", "Нью-Ёрк"));
        assertFalse(BelarusianWordNormalizer.equals("Нью-Ё+рк", "Нью-ёрк" ));
        assertTrue(BelarusianWordNormalizer.equals("ВКПб", "ВКПб"));
    }
}
