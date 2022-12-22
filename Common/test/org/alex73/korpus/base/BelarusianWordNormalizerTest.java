package org.alex73.korpus.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.junit.Test;

public class BelarusianWordNormalizerTest {

    private ILanguage.INormalizer wordNormalizer = LanguageFactory.get("bel").getNormalizer();

    @Test
    public void testHash() {
        assertEquals(wordNormalizer.hash("Ко\u0301"), wordNormalizer.hash("ко"));
        assertEquals(wordNormalizer.hash("Кў"), wordNormalizer.hash("ку"));
        assertEquals(wordNormalizer.hash("яг"), wordNormalizer.hash("ЯҐ"));
    }

    @Test
    public void testEquals() {
        assertTrue(wordNormalizer.equals("Ко\u0301", "Ко\u0301"));
        assertTrue(wordNormalizer.equals("ко\u0301", "Ко\u0301"));
        assertTrue(wordNormalizer.equals("Ко", "Ко"));
        assertTrue(wordNormalizer.equals("Ко", "Ко"));
        assertTrue(wordNormalizer.equals("Менск", "Менск"));
        assertTrue(wordNormalizer.equals("шмат", "Шмат"));
        assertTrue(wordNormalizer.equals("уэлс", "Ўэлс"));
        assertTrue(wordNormalizer.equals("уэлс", "ўэлс"));
        assertTrue(wordNormalizer.equals("Ўэлс", "Ўэлс"));
        assertFalse(wordNormalizer.equals("Ўэлс", "ўэлс"));
        assertFalse(wordNormalizer.equals("Ўэлс", "Уэлс"));
        assertFalse(wordNormalizer.equals("шмат", "шматок"));
        assertFalse(wordNormalizer.equals("шмат", "шматы"));
        assertFalse(wordNormalizer.equals("шматы", "шмат"));
        assertFalse(wordNormalizer.equals("шматок", "шмат"));
        assertFalse(wordNormalizer.equals("шмат", "няшмат"));
        assertFalse(wordNormalizer.equals("шмат", "шамк"));
        assertTrue(wordNormalizer.equals("малы\u0301", "малы\u0301"));
        assertTrue(wordNormalizer.equals("малы\u0301", "малы"));
        assertFalse(wordNormalizer.equals("малы\u0301", "ма\u0301лы"));
        assertTrue(wordNormalizer.equals("малы´", "малы"));
        assertTrue(wordNormalizer.equals("малы´", "малы´"));
        assertFalse(wordNormalizer.equals("малы´", "ма´лы"));
        assertTrue(wordNormalizer.equals("малы", "ма\u0301лы"));
        assertTrue(wordNormalizer.equals("малы", "ма\u0301лы\u0301"));
        assertTrue(wordNormalizer.equals("Нью-Ё\u0301рк", "Нью-Ёрк"));
        assertFalse(wordNormalizer.equals("Нью-Ё\u0301рк", "Нью-ёрк"));
        assertTrue(wordNormalizer.equals("ВКПб", "ВКПб"));
    }
}
