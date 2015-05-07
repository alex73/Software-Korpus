package org.alex73.korpus.client.utils;

import org.alex73.korpus.shared.dto.ResultText;
import org.alex73.korpus.shared.dto.WordResult;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TextPosTest {
    ResultText text;

    @Before
    public void prepareText() {
        text = new ResultText();
        text.words = new WordResult[4][];
        for (int i = 0; i < text.words.length; i++) {
            text.words[i] = new WordResult[i + 1];
        }
        for (int i = 0; i < text.words.length; i++) {
            for (int j = 0; j < text.words[i].length; j++) {
                text.words[i][j] = new WordResult();
            }
        }
    }

    @Test
    public void checkNext() {
        TextPos pos = new TextPos(text, 0, 0);
        for (int i = 0; i < text.words.length; i++) {
            for (int j = 0; j < text.words[i].length; j++) {
                assertEquals(i, pos.getSentence());
                assertEquals(j, pos.getWord());
                pos = pos.addWords(1);
            }
        }
        assertEquals(text.words.length - 1, pos.getSentence());
        assertEquals(text.words[text.words.length - 1].length - 1, pos.getWord());
    }

    @Test
    public void checkPrev() {
        TextPos pos = new TextPos(text, text.words.length - 1, text.words[text.words.length - 1].length - 1);
        for (int i = text.words.length - 1; i >= 0; i--) {
            for (int j = text.words[i].length - 1; j >= 0; j--) {
                assertEquals(i, pos.getSentence());
                assertEquals(j, pos.getWord());
                pos = pos.addWords(-1);
            }
        }
        assertEquals(0, pos.getSentence());
        assertEquals(0, pos.getWord());
    }
}
