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

/**
 * Class for word normalization, i.e. remove upper case, fix apostrophe, replace
 * stress char to '+', change first 'ў' to 'у'.
 */
public class BelarusianWordNormalizer {
    public static final Locale BEL = new Locale("be");
    public static final String letters = "´ёйцукенгшўзх'фывапролджэячсмітьбющиЁЙЦУКЕНГШЎЗХ'ФЫВАПРОЛДЖЭЯЧСМІТЬБЮЩИqwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";
    private static final String ZNAKI = "-—,:!?/.…\"“”«»()[]";

    public static boolean isLetter(char c) {
        return letters.indexOf(c) >= 0;
    }

    public static boolean isZnak(char c) {
        return ZNAKI.indexOf(c) >= 0;
    }

    public static String normalize(String word) {
        if (word == null) {
            return null;
        }
        char[] chars = word.toCharArray();
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
                // Націск: асобны знак - 00B4, спалучэньне з папярэдняй літарай
                // - 0301
                c = '+';
                break;
            }
            chars[i] = c;
        }
        return new String(chars);
    }
}
