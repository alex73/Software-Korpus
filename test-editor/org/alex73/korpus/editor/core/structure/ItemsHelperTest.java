/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2013 Aleś Bułojčyk (alex73mail@gmail.com)
               Home page: https://sourceforge.net/projects/korpus/

 This file is part of Korpus.

 Korpus is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Korpus is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.alex73.korpus.editor.core.structure;

import static org.junit.Assert.assertEquals;

import org.alex73.korpus.editor.core.GrammarDBTestInit;
import org.junit.Before;
import org.junit.Test;

import alex73.corpus.text.W;
import alex73.corpus.text.Z;

public class ItemsHelperTest {
    Line line;

    @Before
    public void before() throws Exception {
        GrammarDBTestInit.initEmpty();

        line = new Line();
        line.add(ItemHelper.createS("  "));
        W w1 = new W();
        w1.setValue("word");
        line.add(ItemHelper.createW("word"));
        Z wz = new Z();
        wz.setValue(".");
        line.add(ItemHelper.createZ("."));
    }

    void check(String... itemTexts) {
        assertEquals(line.size(), itemTexts.length);
        for (int i = 0; i < itemTexts.length; i++) {
            assertEquals(ItemHelper.getText(line.get(i)), itemTexts[i]);
        }
    }

    @Test
    public void noSplit() {
        check("  ", "word", ".");
    }

    @Test
    public void splitSpace() {
        line.splitAt(1);
        check(" ", " ", "word", ".");
    }

    @Test
    public void splitWord() {
        line.splitAt(3);
        check("  ", "w", "ord", ".");
    }

    @Test
    public void splitOnBorders() {
        line.splitAt(0);
        line.splitAt(6);
        line.splitAt(7);
        check("  ", "word", ".");
    }

    @Test(expected = Exception.class)
    public void splitLineWrongLeft() {
        line.leftAt(1);
    }

    @Test(expected = Exception.class)
    public void splitLineWrongRight() {
        line.rightAt(1);
    }

    public void splitLineOkLeft() {
        line = line.leftAt(2);
        check("  ");
    }

    public void splitLineOkRight() {
        line = line.rightAt(2);
        check("word", ".");
    }

    @Test(expected = Exception.class)
    public void insertWrongInside() {
        line.insertItemAt(1, ItemHelper.createS(" "));
    }

    @Test(expected = Exception.class)
    public void insertWrongAfter() {
        line.insertItemAt(100, ItemHelper.createS(" "));
    }

    @Test
    public void insertBefore() {
        line.insertItemAt(0, ItemHelper.createS(" "));
        check(" ", "  ", "word", ".");
    }

    @Test
    public void insertAfter() {
        line.insertItemAt(7, ItemHelper.createS(" "));
        check("  ", "word", ".", " ");
    }

    @Test
    public void insertInside1() {
        line.insertItemAt(2, ItemHelper.createS(" "));
        check("  ", " ", "word", ".");
    }

    @Test
    public void insertInside2() {
        line.insertItemAt(6, ItemHelper.createS(" "));
        check("  ", "word", " ", ".");
    }

    @Test
    public void normalizeSpaces() {
        line.insertItemAt(0, ItemHelper.createS(" "));
        line.normalize();
        check("   ", "word", ".");
    }

    @Test
    public void normalizeWords() {
        line.insertItemAt(2, ItemHelper.createW("tt"));
        line.normalize();
        check("  ", "ttword", ".");
    }

    @Test
    public void normalizeZnak() {
        line.insertItemAt(6, ItemHelper.createZ(","));
        line.normalize();
        check("  ", "word", ",", ".");
    }
}
