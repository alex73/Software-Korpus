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

package org.alex73.korpus.languages.belarusian;

import java.util.Map;

import org.alex73.korpus.languages.ILanguage;

public class BelarusianWordNormalizer implements ILanguage.INormalizer {
    public static final char pravilny_nacisk = '\u0301';
    protected final String usie_naciski = pravilny_nacisk + "\u00B4";
    public static final char pravilny_apostraf = '\u02BC';
    private final String usie_apostrafy = pravilny_apostraf + "\'\u2019";
    private final String letters = usie_naciski + usie_apostrafy
            + "-ёйцукенгшўзхфывапролджэячсмітьъбющиЁЙЦУКЕНГШЎЗХФЫВАПРОЛДЖЭЯЧСМІТЬЪБЮЩИqwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM0123456789";

    protected final int CHARS_LEN = 0x2020;
    protected final char[] ZNAKNORMALIZE = new char[CHARS_LEN];
    protected final char[] LITENORMALIZE = new char[CHARS_LEN];
    protected final char[] SUPERNORMALIZE = new char[CHARS_LEN];

    public BelarusianWordNormalizer() {
        for (char c = 0; c < CHARS_LEN; c++) {
            if (Character.isLetterOrDigit(c)) {
                ZNAKNORMALIZE[c] = c;
                LITENORMALIZE[c] = Character.toLowerCase(c);
            }
        }

        // Пошук
        ZNAKNORMALIZE['?'] = '?';
        ZNAKNORMALIZE['*'] = '*';
        LITENORMALIZE['?'] = '?';
        LITENORMALIZE['*'] = '*';
        // Правільны апостраф - 02BC
        for (char c : usie_apostrafy.toCharArray()) {
            ZNAKNORMALIZE[c] = pravilny_apostraf;
            LITENORMALIZE[c] = pravilny_apostraf;
        }
        // Злучкі
        ZNAKNORMALIZE['-'] = '-';
        LITENORMALIZE['-'] = '-';

        // ґ -> г
        LITENORMALIZE['ґ'] = 'г';
        LITENORMALIZE['Ґ'] = 'Г';

        for (char c = 0; c < CHARS_LEN; c++) {
            SUPERNORMALIZE[c] = LITENORMALIZE[c];
        }
        // дадаткова канвертуем мяккія у цвёрдыя
        for (Map.Entry<Character, Character> en : Map.of('ґ', 'г', 'ў', 'у', 'й', 'і', 'ё', 'о', 'е', 'э', 'я', 'а', 'ю', 'у', 'ь', '\0').entrySet()) {
            SUPERNORMALIZE[en.getKey()] = en.getValue();
            SUPERNORMALIZE[Character.toUpperCase(en.getKey())] = en.getValue();
        }
    }

    @Override
    public int hash(String word) {
        if (word == null) {
            return 0;
        }
        int result = 0;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            c = c < SUPERNORMALIZE.length ? SUPERNORMALIZE[c] : 0;
            if (c > 0) {
                result = 31 * result + c;
            }
        }
        return result;
    }

    @Override
    public String znakNormalized(CharSequence word) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            c = c < ZNAKNORMALIZE.length ? ZNAKNORMALIZE[c] : 0;
            if (c > 0) {
                str.append(c);
            }
        }
        return str.toString();
    }

    @Override
    public String lightNormalized(CharSequence word) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            c = c < LITENORMALIZE.length ? LITENORMALIZE[c] : 0;
            if (c > 0) {
                if (str.length() == 0) {
                    switch (c) {
                    case 'ў':
                        c = 'у';
                        break;
                    case 'й':
                        c = 'і';
                        break;
                    }
                }
                str.append(c);
            }
        }
        return str.toString();
    }

    @Override
    public String superNormalized(String word) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            c = c < SUPERNORMALIZE.length ? SUPERNORMALIZE[c] : 0;
            if (c > 0) {
                str.append(c);
            }
        }
        return str.toString();
    }

    @Override
    public boolean isApostraf(char c) {
        return usie_apostrafy.indexOf(c) >= 0;
    }

    @Override
    public boolean isLetter(char c) {
        return letters.indexOf(c) >= 0;
    }
}
