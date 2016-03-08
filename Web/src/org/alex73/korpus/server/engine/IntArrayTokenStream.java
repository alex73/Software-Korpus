/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2014 Aleś Bułojčyk (alex73mail@gmail.com)
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
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.analysis.NumericTokenStream.NumericTermAttribute;
import org.apache.lucene.analysis.TokenStream;

/**
 * Token stream which returns each int as separate token.
 */
public class IntArrayTokenStream extends TokenStream {
    final Set<Integer> data;
    final int precisionStep;
    final int shift;
    Iterator<Integer> it;
    int index;
    final NumericTermAttribute token = addAttribute(NumericTermAttribute.class);

    public IntArrayTokenStream(Set<Integer> data, int precisionStep, int shift) {
        this.data = data;
        this.precisionStep = precisionStep;
        this.shift = shift;
    }

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();
        if (it.hasNext()) {
            token.init(it.next(), 16, precisionStep, shift);
            index++;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() throws IOException {
        it = data.iterator();
    }
}
