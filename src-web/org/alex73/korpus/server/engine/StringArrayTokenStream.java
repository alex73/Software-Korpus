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

package org.alex73.korpus.server.engine;

import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 * Token stream which returns each string as separate token.
 */
public class StringArrayTokenStream extends TokenStream {
    final String[] data;
    int index;
    final CharTermAttribute token = addAttribute(CharTermAttribute.class);
    final OffsetAttribute offset = addAttribute(OffsetAttribute.class);

    public StringArrayTokenStream(String[] data) {
        this.data = data;
    }

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();
        if (index < data.length) {
            token.setEmpty().append(data[index]);
            offset.setOffset(0, data[index].length());
            index++;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() throws IOException {
        index = 0;
    }
}
