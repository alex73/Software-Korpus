/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2015 Aleś Bułojčyk (alex73mail@gmail.com)
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

import alex73.corpus.text.InlineTag;
import alex73.corpus.text.O;
import alex73.corpus.text.S;
import alex73.corpus.text.W;
import alex73.corpus.text.Z;

public class ItemHelper {

    public static S createS(String spaceChar) {
        S s = new S();
        s.setChar(spaceChar);
        return s;
    }

    public static Z createZ(String znak) {
        Z z = new Z();
        z.setValue(znak);
        return z;
    }

    public static InlineTag createInlineTag(String tag) {
        InlineTag t = new InlineTag();
        t.setValue(tag);
        return t;
    }

    public static W createW(String word) {
        W w = new W();
        w.setValue(word);
        return w;
    }

    public static String getText(Object o) {
        if (o instanceof W) {
            return ((W) o).getValue();
        } else if (o instanceof Z) {
            return ((Z) o).getValue();
        } else if (o instanceof S) {
            return ((S) o).getChar();
        } else if (o instanceof O) {
            return ((O) o).getValue();
        } else if (o instanceof LongTagItem) {
            return ((LongTagItem) o).getText();
        } else if (o instanceof SentenceSeparatorItem) {
            return ((SentenceSeparatorItem) o).getText();
        } else {
            throw new RuntimeException("Wrong object type");
        }
    }

    public static Object splitLeft(Object o, int pos) {
        if (o instanceof W) {
            String text = ((W) o).getValue();
            return createW(text.substring(0, pos));
        } else if (o instanceof S) {
            String text = ((S) o).getChar();
            return createS(text.substring(0, pos));
        } else {
            throw new RuntimeException("Wrong object type");
        }
    }

    public static Object splitRight(Object o, int pos) {
        if (o instanceof W) {
            String text = ((W) o).getValue();
            return createW(text.substring(pos));
        } else if (o instanceof S) {
            String text = ((S) o).getChar();
            return createS(text.substring(pos));
       } else {
            throw new RuntimeException("Wrong object type");
        }
    }
}
