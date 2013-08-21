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

package org.alex73.korpus.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Class for word normalization, i.e. remove stress and upper case.
 * 
 * This class caches responses for better performance.
 */
public class WordNormalizer {
    public static final Locale BEL = new Locale("be");
    
    static private final Map<String, String> NORMALIZED = Collections.synchronizedMap(new HashMap<String, String>());
    
    public static String normalize(String word) {
        String n = NORMALIZED.get(word);
        if (n == null) {
            n = StressUtils.unstress(word.trim().toLowerCase(BEL));
            NORMALIZED.put(word, n.equals(word) ? word : n);
        }
        return n;
    }
}
