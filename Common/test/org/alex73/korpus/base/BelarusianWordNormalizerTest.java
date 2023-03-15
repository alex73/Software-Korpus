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
    public void testZnakNormalized() {
        assertFalse(wordNormalizer.znakNormalized("ко\u0301").equals(wordNormalizer.znakNormalized("Ко\u0301")));
        assertTrue(wordNormalizer.znakNormalized("ко\u02BC").equals(wordNormalizer.znakNormalized("ко\u2019")));
    }

    @Test
    public void testLightNormalized() {
        assertTrue(wordNormalizer.lightNormalized("Ко\u0301").equals(wordNormalizer.lightNormalized("Ко\u0301")));
        assertTrue(wordNormalizer.lightNormalized("ко\u0301").equals(wordNormalizer.lightNormalized("Ко\u0301")));
        assertTrue(wordNormalizer.lightNormalized("Ко").equals(wordNormalizer.lightNormalized("Ко")));
        assertTrue(wordNormalizer.lightNormalized("Ко").equals(wordNormalizer.lightNormalized("Ко")));
        assertTrue(wordNormalizer.lightNormalized("Менск").equals(wordNormalizer.lightNormalized("Менск")));
        assertTrue(wordNormalizer.lightNormalized("шмат").equals(wordNormalizer.lightNormalized("Шмат")));
        assertTrue(wordNormalizer.lightNormalized("уэлс").equals(wordNormalizer.lightNormalized("Ўэлс")));
        assertTrue(wordNormalizer.lightNormalized("уэлс").equals(wordNormalizer.lightNormalized("ўэлс")));
        assertTrue(wordNormalizer.lightNormalized("Ўэлс").equals(wordNormalizer.lightNormalized("Ўэлс")));
        assertTrue(wordNormalizer.lightNormalized("Ўэлс").equals(wordNormalizer.lightNormalized("ўэлс")));
        assertFalse(wordNormalizer.lightNormalized("шмат").equals(wordNormalizer.lightNormalized("шматок")));
        assertFalse(wordNormalizer.lightNormalized("шмат").equals(wordNormalizer.lightNormalized("шматы")));
        assertFalse(wordNormalizer.lightNormalized("шматы").equals(wordNormalizer.lightNormalized("шмат")));
        assertFalse(wordNormalizer.lightNormalized("шматок").equals(wordNormalizer.lightNormalized("шмат")));
        assertFalse(wordNormalizer.lightNormalized("шмат").equals(wordNormalizer.lightNormalized("няшмат")));
        assertFalse(wordNormalizer.lightNormalized("шмат").equals(wordNormalizer.lightNormalized("шамк")));
        assertTrue(wordNormalizer.lightNormalized("малы\u0301").equals(wordNormalizer.lightNormalized("малы\u0301")));
        assertTrue(wordNormalizer.lightNormalized("малы\u0301").equals(wordNormalizer.lightNormalized("малы")));
        assertTrue(wordNormalizer.lightNormalized("малы\u0301").equals(wordNormalizer.lightNormalized("ма\u0301лы")));
        assertTrue(wordNormalizer.lightNormalized("малы´").equals(wordNormalizer.lightNormalized("малы")));
        assertTrue(wordNormalizer.lightNormalized("малы´").equals(wordNormalizer.lightNormalized("малы´")));
        assertTrue(wordNormalizer.lightNormalized("малы´").equals(wordNormalizer.lightNormalized("ма´лы")));
        assertTrue(wordNormalizer.lightNormalized("малы").equals(wordNormalizer.lightNormalized("ма\u0301лы")));
        assertTrue(wordNormalizer.lightNormalized("малы").equals(wordNormalizer.lightNormalized("ма\u0301лы\u0301")));
        assertTrue(wordNormalizer.lightNormalized("Нью-Ё\u0301рк").equals(wordNormalizer.lightNormalized("Нью-Ёрк")));
        assertTrue(wordNormalizer.lightNormalized("Нью-Ё\u0301рк").equals(wordNormalizer.lightNormalized("Нью-ёрк")));
        assertTrue(wordNormalizer.lightNormalized("ВКПб").equals(wordNormalizer.lightNormalized("ВКПб")));
    }

    @Test
    public void testSuperNormalized() {
        assertTrue(wordNormalizer.superNormalized("сьнег").equals(wordNormalizer.superNormalized("снег")));
        assertTrue(wordNormalizer.superNormalized("плян").equals(wordNormalizer.superNormalized("план")));
    }
}
