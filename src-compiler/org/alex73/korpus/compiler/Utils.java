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

package org.alex73.korpus.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import alex73.corpus.paradigm.P;

public class Utils {

    static Random RANDOM = new Random();

    /**
     * Parts of text should be randomized against ability to restore original book.
     */
    static List<P> randomizeOrder(List<P> sentences) {
        List<P> result = new ArrayList<>(sentences.size());
        while (!sentences.isEmpty()) {
            int next = RANDOM.nextInt(sentences.size());
            result.add(sentences.remove(next));
        }
        return result;
    }
}
