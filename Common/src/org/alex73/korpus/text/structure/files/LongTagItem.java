/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2015 Aleś Bułojčyk (alex73mail@gmail.com)

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

package org.alex73.korpus.text.structure.files;

public class LongTagItem implements ITextLineElement {
    private String text;

    public LongTagItem(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LongTagItem) {
            LongTagItem o = (LongTagItem) obj;
            return text.equals(o.text);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return text;
    }
}
