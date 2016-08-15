package org.alex73.korpus.base;

import java.util.Comparator;

import org.alex73.korpus.utils.StressUtils;

public class BelarusianComparators {

    public static final Comparator<String> FULL = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            int v = WITHOUT_STRESS.compare(o1, o2);
            if (v == 0) {
                v = WITH_STRESS.compare(o1, o2);
            }
            return v;
        }
    };
    public static final Comparator<String> FULL_REVERSE = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            StringBuilder s1 = new StringBuilder(o1);
            StringBuilder s2 = new StringBuilder(o2);
            s1.reverse();
            s2.reverse();
            return FULL.compare(s1.toString(), s2.toString());
        }
    };
    public static final Comparator<String> WITHOUT_STRESS = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            o1 = o1.toLowerCase();
            o2 = o2.toLowerCase();
            int p1 = 0;
            int p2 = 0;
            char c1, c2;
            while (true) {
                do {
                    c1 = p1 < o1.length() ? o1.charAt(p1++) : 0;
                } while (c1 == StressUtils.STRESS_CHAR);
                do {
                    c2 = p2 < o2.length() ? o2.charAt(p2++) : 0;
                } while (c2 == StressUtils.STRESS_CHAR);

                if (c1 != 0 && c2 != 0) {
                    if (c1 == c2) {
                        continue;
                    }
                    return compareChars(c1, c2);
                } else if (c1 == 0 && c2 == 0) {
                    return 0;
                } else if (c1 == 0) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }
    };
    public static final Comparator<String> WITH_STRESS = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            o1 = o1.toLowerCase();
            o2 = o2.toLowerCase();
            int p1 = 0;
            int p2 = 0;
            char c1, c2;
            while (true) {
                c1 = p1 < o1.length() ? o1.charAt(p1++) : 0;
                c2 = p2 < o2.length() ? o2.charAt(p2++) : 0;

                if (c1 != 0 && c2 != 0) {
                    if (c1 == c2) {
                        continue;
                    }
                    return compareChars(c1, c2);
                } else if (c1 == 0 && c2 == 0) {
                    return 0;
                } else if (c1 == 0) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }
    };
    static final String LETTERS = "абвгдеёжзіклмнопрстуўфхцчшыьэюя";

    static int compareChars(char c1, char c2) {
        int p1 = LETTERS.indexOf(c1);
        int p2 = LETTERS.indexOf(c2);
        return p1 - p2;
    }
}
