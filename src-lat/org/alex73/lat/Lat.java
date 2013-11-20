/**
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.


    Author: Aleś Bułojčyk <alex73mail@gmail.com>
    Homepage: http://sourceforge.net/projects/korpus/
 */
package org.alex73.lat;

import java.util.HashMap;
import java.util.Map;

public class Lat {
    /**
     * Лацінізаваць кірылічны тэкст.
     * 
     * @param latTrad
     *            true : традыцыйная лацінка, false : афіцыйная лацінка (згодна
     *            https://be-x-old.wikipedia.org/wiki/Інструкцыя_па_трансьлітарацыі)
     */
    public static String lat(String text, boolean latTrad) {
        String out = "";
        Map<Character, String> simple = new HashMap<>();
        simple.put('а', "a");
        simple.put('б', "b");
        simple.put('в', "v");
        simple.put('г', "h");
        simple.put('ґ', "g");
        simple.put('д', "d");
        simple.put('ж', "ž");
        simple.put('з', "z");
        simple.put('й', "j");
        simple.put('к', "k");
        simple.put('м', "m");
        simple.put('н', "n");
        simple.put('о', "o");
        simple.put('п', "p");
        simple.put('р', "r");
        simple.put('с', "s");
        simple.put('т', "t");
        simple.put('у', "u");
        simple.put('ў', "ŭ");
        simple.put('ф', "f");
        simple.put('х', "ch");
        simple.put('ц', "c");
        simple.put('ч', "č");
        simple.put('ш', "š");
        simple.put('ы', "y");
        simple.put('э', "e");
        Map<Character, String> halosnyja = new HashMap<>();
        halosnyja.put('е', "e");
        halosnyja.put('ё', "o");
        halosnyja.put('ю', "u");
        halosnyja.put('я', "a");

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char prev = i > 0 ? text.charAt(i - 1) : '?';
            char next = i < (text.length() - 1) ? text.charAt(i + 1) : '?';
            boolean wordUpper = isUW(c, prev, next);
            boolean thisUpper = isU(c);
            boolean prevUpper = isU(prev);
            c = Character.toLowerCase(c);
            prev = Character.toLowerCase(prev);
            next = Character.toLowerCase(next);

            if (c == '\'' || c == '’') {
                continue;
            }
            String sm = simple.get(c);
            if (sm == null) {
                sm = "";
                switch (c) {
                case 'л':
                    if (latTrad) {
                        if (oneOf(next, "еёюяіь\'’")) {
                            sm = "l";
                        } else {
                            sm = "ł";
                        }
                    } else {
                        sm = "l";
                    }
                    break;
                case 'е':
                case 'ё':
                case 'ю':
                case 'я':
                    if (prev == 'л' && latTrad) {
                        sm = halosnyja.get(c);
                    } else if (prev == '\'' || prev == '’' || prev == 'й' || prev == 'ў') {
                        sm = 'j' + halosnyja.get(c);
                    } else if (isZyc(prev)) {
                        sm = 'i' + halosnyja.get(c);
                    } else {
                        sm = 'j' + halosnyja.get(c);
                    }
                    break;
                case 'і':
                    if (prev == '\'' || prev == '’') {
                        sm = "ji";
                    } else if (prev == 'й' || prev == 'ў') {
                        if (latTrad) {
                            sm = "ji";
                        } else {
                            sm = "i";
                        }
                    } else {
                        sm = "i";
                    }
                    break;
                case 'ь':
                    sm = "";
                    if (out.length() > 0) {
                        char p = out.charAt(out.length() - 1);
                        switch (p) {
                        case 'Z':
                            p = 'Ź';
                            break;
                        case 'z':
                            p = 'ź';
                            break;
                        case 'N':
                            p = 'Ń';
                            break;
                        case 'n':
                            p = 'ń';
                            break;
                        case 'S':
                            p = 'Ś';
                            break;
                        case 's':
                            p = 'ś';
                            break;
                        case 'C':
                            p = 'Ć';
                            break;
                        case 'c':
                            p = 'ć';
                            break;
                        case 'L':
                            if (!latTrad) {
                                p = 'Ĺ';
                            }
                            break;
                        case 'l':
                            if (!latTrad) {
                                p = 'ĺ';
                            }
                            break;
                        case 'Ł':
                            if (latTrad) {
                                p = 'L';
                            }
                            break;
                        case 'ł':
                            if (latTrad) {
                                p = 'l';
                            }
                            break;
                        }
                        out = changeLastLetter(out, p);
                    }
                    break;
                default:
                    sm = "" + c;
                    break;
                }
            }

            if (thisUpper) {
                if (wordUpper || sm.length() < 2) {
                    sm = sm.toUpperCase();
                } else {
                    sm = Character.toUpperCase(sm.charAt(0)) + sm.substring(1);
                }
            }
            out += sm;
        }
        return out;
    }

    /**
     * Вялікая літара ?
     */
    private static boolean isU(char c) {
        return c == Character.toUpperCase(c) && c != Character.toLowerCase(c);
    }

    /**
     * Вялікая літара пабач зь іншай вялікай літарай ?
     */
    private static boolean isUW(char c, char prev, char next) {
        return isU(c) && (isU(prev) || isU(next));
    }

    /**
     * Літара адна з ... ?
     */
    private static boolean oneOf(char letter, String many) {
        return many.indexOf(letter) >= 0;
    }

    /**
     * Зьмяніць апошнюю літару.
     */
    private static String changeLastLetter(String text, char newLetter) {
        return text.substring(0, text.length() - 1) + newLetter;
    }

    /**
     * Ці літара - зычная ?
     */
    private static boolean isZyc(char c) {
        return oneOf(Character.toLowerCase(c), "йцкнгшўзхфвпрлджчсмтб");
    }

    /**
     * Ці літара - галосная ?
     */
    private static boolean isHal(char c) {
        return oneOf(Character.toLowerCase(c), "ёуеыаоэяію");
    }

    /**
     * Прыбраць гачыкі з лацінкавага тэксту
     */
    public static String unhac(String text) {
        return text.replace('Ć', 'C').replace('ć', 'c').replace('Č', 'C').replace('č', 'c').replace('Ł', 'L')
                .replace('ł', 'l').replace('Ĺ', 'L').replace('ĺ', 'l').replace('Ń', 'N').replace('ń', 'n')
                .replace('Ś', 'S').replace('ś', 's').replace('Š', 'S').replace('š', 's').replace('Ŭ', 'U')
                .replace('ŭ', 'u').replace('Ź', 'Z').replace('ź', 'z').replace('Ž', 'Z').replace('ž', 'z');
    }
}
