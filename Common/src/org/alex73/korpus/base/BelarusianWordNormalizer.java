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

package org.alex73.korpus.base;

import java.util.Locale;

public class BelarusianWordNormalizer {
    public static final Locale BEL = new Locale("be");
    public static final String apostrafy = "\'\u02BC\u2019";
    public static final String naciski = "+\u00B4\u0301";
    public static final String letters = apostrafy + naciski
            + "-ёйцукенгшўзхфывапролджэячсмітьбющиЁЙЦУКЕНГШЎЗХФЫВАПРОЛДЖЭЯЧСМІТЬБЮЩИqwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM0123456789";
    private static final String ZNAKI = "—,:!?/.…\"“”«»()[]";

    private static final int CHARS_LEN = 0x2020;
    // Максімальная нармалізацыя - прывядзенне да агульнага хэшу ці для індэксацыі ў
    // Lucene. Патрабуе абавязковай дадатковай праверкі.
    private static final char[] SUPERNORMALIZE = new char[CHARS_LEN];
    // Мінімальная нармалізацыя - толькі націскі і апострафы да правільнай формы.
    private static final char[] LITENORMALIZE = new char[CHARS_LEN];
    // Ператварэнне ў малыя літары
    private static final char[] UMALYJA = new char[CHARS_LEN];
    // Ці вялікая літара
    private static final boolean[] CIVIALIKIJA = new boolean[CHARS_LEN];

    static {
        for (char c = 0; c < CHARS_LEN; c++) {
            if (Character.isLetterOrDigit(c)) {
                SUPERNORMALIZE[c] = Character.toLowerCase(c);
                UMALYJA[c] = Character.toLowerCase(c);
                LITENORMALIZE[c] = c;
            }
            CIVIALIKIJA[c] = Character.isUpperCase(c);
        }
        UMALYJA['+'] = '+';
        UMALYJA['\''] = '\'';
        UMALYJA['-'] = '-';

        LITENORMALIZE['ґ'] = 'г'; // ґ -> г
        LITENORMALIZE['Ґ'] = 'Г';
        // Правільны апостраф - 02BC, але паўсюль ужываем лацінкавы
        LITENORMALIZE['\''] = '\'';
        LITENORMALIZE['\u02BC'] = '\'';
        LITENORMALIZE['\u2019'] = '\'';
        // Націск - '+'
        LITENORMALIZE['+'] = '+';
        LITENORMALIZE['\u00B4'] = '+';
        LITENORMALIZE['\u0301'] = '+';
        LITENORMALIZE['-'] = '-';
        // пошук
        LITENORMALIZE['?'] = '?';
        LITENORMALIZE['*'] = '*';

        SUPERNORMALIZE['ґ'] = 'г'; // ґ -> г
        SUPERNORMALIZE['Ґ'] = 'г';
        SUPERNORMALIZE['ў'] = 'у'; // ў -> у
        SUPERNORMALIZE['Ў'] = 'у';
        // Правільны апостраф - 02BC, але паўсюль ужываем лацінкавы
        SUPERNORMALIZE['\''] = '\'';
        SUPERNORMALIZE['\u02BC'] = '\'';
        SUPERNORMALIZE['\u2019'] = '\'';
        SUPERNORMALIZE['-'] = '-';
    }

    public static int hash(String word) {
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

    public static String lightNormalized(String word) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            c = c < LITENORMALIZE.length ? LITENORMALIZE[c] : 0;
            if (c > 0) {
                str.append(c);
            }
        }
        return str.toString();
    }

    public static String superNormalized(String word) {
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

    /**
     * Параўноўвае слова ў базе(ці ўведзенае карыстальнікам слова) з словам у
     * тэксце. Тут правяраюцца ўсе "несупярэчнасці": націскі, вялікія літары,
     * апострафы, Г выбухное, Ў.
     * 
     * Націскі могуць быць альбо не быць як у базе, так і ў тэксце.
     */
    public static boolean equals(String dbWord, String anyWord) {
        /* Націск супаў у той самай пазіцыі. */
        byte stressWasEquals = 0;
        /* Націск з базы быў прапушчаны. */
        byte stressWasMissedInWord = 0;
        /* Націск у баз прапушчаны. */
        byte stressWasMissedInDb = 0;
        for (int iDb = 0, iAny = 0;; iDb++, iAny++) {
            char cDb = iDb < dbWord.length() ? dbWord.charAt(iDb) : Character.MAX_VALUE;
            if (cDb < LITENORMALIZE.length) {
                cDb = LITENORMALIZE[cDb];
            } else if (cDb != Character.MAX_VALUE) {
                cDb = 0;
            }
            char cAny = iAny < anyWord.length() ? anyWord.charAt(iAny) : Character.MAX_VALUE;
            if (cAny < LITENORMALIZE.length) {
                cAny = LITENORMALIZE[cAny];
            } else if (cAny != Character.MAX_VALUE) {
                cAny = 0;
            }
            if (cDb == Character.MAX_VALUE && cAny == Character.MAX_VALUE) {
                return stressWasEquals + stressWasMissedInWord + stressWasMissedInDb <= 1;
            }
            if (cAny == Character.MAX_VALUE || cDb == Character.MAX_VALUE) {
                if (cDb == '+') {
                    stressWasMissedInWord = 1;
                    continue;
                } else if (cAny == '+') {
                    stressWasMissedInDb = 1;
                    continue;
                }
                return false;
            }
            if (cAny == 0 || cDb == 0) {
                return false;
            }
            if (iDb == 0 && iAny == 0) {
                // першы сімвал - можа вялікі ?
                boolean vialikiDb = CIVIALIKIJA[cDb];
                boolean vialikiAny = CIVIALIKIJA[cAny];
                if (vialikiDb && !vialikiAny) {
                    return false;
                }
                cDb = UMALYJA[cDb];
                cAny = UMALYJA[cAny];
                // першы сімвал - можа у/ў?
                if (cDb == 'ў') {
                    if (cAny == 'ў') {
                        continue;
                    } else {
                        return false;
                    }
                }
                if (cAny == 'ў') {
                    cAny = 'у';
                }
            } else {
                cAny = UMALYJA[cAny];
            }
            if (cDb == '+' && cAny == '+') {
                stressWasEquals = 1;
                continue;
            } else if (cDb == '+') {
                stressWasMissedInWord = 1;
                iAny--;
                continue;
            } else if (cAny == '+') {
                stressWasMissedInDb = 1;
                iDb--;
                continue;
            }
            if (cDb != cAny) {
                return false;
            }
        }
    }

    public static boolean isLetter(char c) {
        return letters.indexOf(c) >= 0;
    }

    @Deprecated // all other than letters
    public static boolean isZnak(char c) {
        return ZNAKI.indexOf(c) >= 0;
    }

    @Deprecated // use equals
    public static String normalizeLowerCase(String word) {
        if (word == null) {
            return null;
        }
        char[] chars = word.toCharArray();
        int outStart = 0;
        int outEnd = 0;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            switch (c) {
            case 'а':
            case 'А':
                c = 'а';
                break;
            case 'б':
            case 'Б':
                c = 'б';
                break;
            case 'в':
            case 'В':
                c = 'в';
                break;
            case 'г':
            case 'Г':
                c = 'г';
                break;
            case 'ґ':
            case 'Ґ':
                c = 'г';
                break;
            case 'д':
            case 'Д':
                c = 'д';
                break;
            case 'е':
            case 'Е':
                c = 'е';
                break;
            case 'ё':
            case 'Ё':
                c = 'ё';
                break;
            case 'ж':
            case 'Ж':
                c = 'ж';
                break;
            case 'з':
            case 'З':
                c = 'з';
                break;
            case 'і':
            case 'І':
                c = 'і';
                break;
            case 'й':
            case 'Й':
                c = 'й';
                break;
            case 'к':
            case 'К':
                c = 'к';
                break;
            case 'л':
            case 'Л':
                c = 'л';
                break;
            case 'м':
            case 'М':
                c = 'м';
                break;
            case 'н':
            case 'Н':
                c = 'н';
                break;
            case 'о':
            case 'О':
                c = 'о';
                break;
            case 'п':
            case 'П':
                c = 'п';
                break;
            case 'р':
            case 'Р':
                c = 'р';
                break;
            case 'с':
            case 'С':
                c = 'с';
                break;
            case 'т':
            case 'Т':
                c = 'т';
                break;
            case 'у':
            case 'У':
                c = 'у';
                break;
            case 'ў':
            case 'Ў':
                c = i == 0 ? 'у' : 'ў';
                break;
            case 'ф':
            case 'Ф':
                c = 'ф';
                break;
            case 'х':
            case 'Х':
                c = 'х';
                break;
            case 'ц':
            case 'Ц':
                c = 'ц';
                break;
            case 'ч':
            case 'Ч':
                c = 'ч';
                break;
            case 'ш':
            case 'Ш':
                c = 'ш';
                break;
            case 'ы':
            case 'Ы':
                c = 'ы';
                break;
            case 'ь':
            case 'Ь':
                c = 'ь';
                break;
            case 'э':
            case 'Э':
                c = 'э';
                break;
            case 'ю':
            case 'Ю':
                c = 'ю';
                break;
            case 'я':
            case 'Я':
                c = 'я';
                break;
            case '\'':
            case '\u02BC':
            case '\u2019':
                // Правільны апостраф - 02BC, але паўсюль ужываем лацінкавы
                c = '\'';
                break;
            case '\u00B4':
            case '\u0301':
            case '+':
                // Націск: асобны знак - 00B4, спалучэньне з папярэдняй літарай
                // - 0301
                c = '+';
                break;
            }
            chars[outEnd] = c;
            outEnd++;
        }
        if (outStart < outEnd && apostrafy.indexOf(chars[outStart]) >= 0) {
            outStart++;
        }
        if (outEnd > 0 && apostrafy.indexOf(chars[outEnd - 1]) >= 0) {
            outEnd--;
        }
        if (outEnd == 0) {
            return "";
        }
        return new String(chars, outStart, outEnd - outStart);
    }

    @Deprecated
    private static final String NORMALIZE_PRESERVE_CASE_ALLOWED = "ёйцукенгшўзхфывапролджэячсмітьбюЁЙЦУКЕНГШЎЗХФЫВАПРОЛДЖЭЯЧСМІТЬБЮ";

    @Deprecated // use equals
    public static String normalizePreserveCase(String word) {
        if (word == null) {
            return null;
        }
        char[] chars = word.toCharArray();
        int outStart = 0;
        int outEnd = 0;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (NORMALIZE_PRESERVE_CASE_ALLOWED.indexOf(c) < 0) {
                switch (c) {
                case 'ґ':
                case 'Ґ':
                    c = 'г';
                    break;
                case '\'':
                case '\u02BC':
                case '\u2019':
                    // Правільны апостраф - 02BC, але паўсюль ужываем лацінкавы
                    c = '\'';
                    break;
                case '\u00B4':
                case '\u0301':
                case '+':
                    // Націск: асобны знак - 00B4, спалучэньне з папярэдняй літарай
                    // - 0301
                    // адкідаем націскі
                    c = '+';
                    break;
                }
            }
            chars[outEnd] = c;
            outEnd++;
        }
        if (outStart < outEnd && apostrafy.indexOf(chars[outStart]) >= 0) {
            outStart++;
        }
        if (outEnd > 0 && apostrafy.indexOf(chars[outEnd - 1]) >= 0) {
            outEnd--;
        }
        if (outEnd == 0) {
            return "";
        }
        return new String(chars, outStart, outEnd - outStart);
    }
}
