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

import org.alex73.korpus.base.BelarusianTags;

/**
 * Some utilities methods for stress processing.
 * 
 * Stress syll index started from 0.
 */
public class StressUtils {

    public static char STRESS_CHAR = '+';

    public static String unstress(String stressedWord) {
        return stressedWord.replace("" + STRESS_CHAR, "");
    }

    public static boolean hasStress(String word) {
        return word.indexOf(STRESS_CHAR) >= 0;
    }

    /**
     * Find stress syll by ё, о using possible value. If possible < 0 - find
     * first.
     */
    public static int getUsuallyStressedSyll(String word, int possible) {
        int r = 0;
        if (possible >= 0) {
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (possible == r && BelarusianTags.USUALLY_STRESSED.indexOf(c) >= 0) {
                    return r;
                }
                if (BelarusianTags.HALOSNYJA.indexOf(c) >= 0) {
                    r++;
                }
            }
        }
        r = 0;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (BelarusianTags.USUALLY_STRESSED.indexOf(c) >= 0) {
                return r;
            }
            if (BelarusianTags.HALOSNYJA.indexOf(c) >= 0) {
                r++;
            }
        }
        return -1;
    }

    public static String setUsuallyStress(String word) {
        if (hasStress(word)) {
            return word;
        }
        if (syllCount(word) == 1) {
            return setStressFromStart(word, 0);
        }
        int u = getUsuallyStressedSyll(word, -1);
        if (u >= 0) {
            return setStressFromStart(word, u);
        }
        return word;
    }

    public static int getStressFromStart(String word) {
        int r = 0;
        for (int i = 0; i < word.length() - 1; i++) {
            char c = word.charAt(i);
            char c1 = word.charAt(i + 1);
            if (c1 == STRESS_CHAR) {
                return r;
            }
            boolean halosnaja = BelarusianTags.HALOSNYJA.indexOf(c) >= 0;
            if (halosnaja) {
                r++;
            }
        }
        return -1;
    }

    public static boolean isAssignable(String destination, String withStress) {
        if (destination.equals(unstress(destination))) {
            return destination.equals(unstress(withStress));
        } else {
            return destination.equals(withStress);
        }
    }

    public static int getStressFromEnd(String word) {
        int r = 0;
        for (int i = word.length() - 1; i >= 0; i--) {
            char c = word.charAt(i);
            if (c == STRESS_CHAR) {
                return r;
            }
            boolean halosnaja = BelarusianTags.HALOSNYJA.indexOf(c) >= 0;
            if (halosnaja) {
                r++;
            }
        }
        return -1;
    }

    public static String setStressFromStart(String word, int pos) {
        if (pos < 0) {
            return word;
        }
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            boolean halosnaja = BelarusianTags.HALOSNYJA.indexOf(c) >= 0;
            if (halosnaja) {
                if (pos == 0) {
                    return word.substring(0, i + 1) + STRESS_CHAR + word.substring(i + 1);
                } else {
                    pos--;
                }
            }
        }
        return word;
    }

    public static String setStressFromEnd(String word, int pos) {
        if (pos < 0) {
            return word;
        }
        for (int i = word.length() - 1; i >= 0; i--) {
            char c = word.charAt(i);
            boolean halosnaja = BelarusianTags.HALOSNYJA.indexOf(c) >= 0;
            if (halosnaja) {
                if (pos == 0) {
                    return word.substring(0, i + 1) + STRESS_CHAR + word.substring(i + 1);
                } else {
                    pos--;
                }
            }
        }
        return word;
    }

    public static void checkStress(String word) throws Exception {
        for (String w : word.split("[\\-, \\.]")) {
            int pos = -1;
            int mainStresses = 0;
            while ((pos = w.indexOf(STRESS_CHAR, pos + 1)) >= 0) {
                if (BelarusianTags.HALOSNYJA.indexOf(w.charAt(pos - 1)) < 0) {
                    throw new Exception("Націск не на галосную");
                }
                mainStresses++;
            }
            if (mainStresses > 1) {
                throw new Exception("Зашмат асноўных націскаў у " + word);
            }
        }
    }

    public static int syllCount(String word) {
        int r = 0;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            boolean halosnaja = BelarusianTags.HALOSNYJA.indexOf(c) >= 0;
            if (halosnaja) {
                r++;
            }
        }
        return r;
    }

    public static char syllHal(String word, int pos) {
        int r = 0;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            boolean halosnaja = BelarusianTags.HALOSNYJA.indexOf(c) >= 0;
            if (halosnaja) {
                if (pos == r) {
                    return c;
                }
                r++;
            }
        }
        return 0;
    }

    public static String setSyllHal(String word, int pos, char cr) {
        int r = 0;
        StringBuilder s=new StringBuilder(word);
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            boolean halosnaja = BelarusianTags.HALOSNYJA.indexOf(c) >= 0;
            if (halosnaja) {
                if (pos == r) {
                    s.setCharAt(i, cr);
                    return s.toString();
                }
                r++;
            }
        }
        throw new RuntimeException("No syll #" + pos + " in the " + word);
    }

    public static String combineAccute(String word) {
        return word.replace(STRESS_CHAR, '\u0301');
    }
}
