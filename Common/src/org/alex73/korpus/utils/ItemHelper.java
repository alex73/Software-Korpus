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

package org.alex73.korpus.utils;

import org.alex73.korpus.text.xml.ITextLineElement;
import org.alex73.korpus.text.xml.O;
import org.alex73.korpus.text.xml.S;
import org.alex73.korpus.text.xml.W;

public class ItemHelper {

    public static ITextLineElement splitLeft(ITextLineElement o, int pos) {
        String text = o.getText().substring(0, pos);
        if (o instanceof W) {
            return new W(text);
        } else if (o instanceof S) {
            return new S(text);
        } else if (o instanceof O) {
            return new O(((O) o).getType(), text);
        } else {
            throw new RuntimeException("Wrong object type");
        }
    }

    public static ITextLineElement splitRight(ITextLineElement o, int pos) {
        String text = o.getText().substring(pos);
        if (o instanceof W) {
            return new W(text);
        } else if (o instanceof S) {
            return new S(text);
        } else if (o instanceof O) {
            return new O(((O) o).getType(), text);
        } else {
            throw new RuntimeException("Wrong object type");
        }
    }
}