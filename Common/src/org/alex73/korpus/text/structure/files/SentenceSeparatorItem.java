/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2013 Aleś Bułojčyk (alex73mail@gmail.com)

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

public class SentenceSeparatorItem implements ITextLineElement {

    public static final char SENTENCE_SEPARATOR_CHAR = '\u21A4';//'�';

    @Override
    public String getText() {
        return "" + SENTENCE_SEPARATOR_CHAR;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SentenceSeparatorItem) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return getText();
    }
}
