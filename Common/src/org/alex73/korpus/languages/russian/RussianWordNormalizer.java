package org.alex73.korpus.languages.russian;

import org.alex73.korpus.languages.ILanguage;

public class RussianWordNormalizer implements ILanguage.INormalizer {
    public static final char pravilny_nacisk = '\u0301';
    public static final String usie_naciski = pravilny_nacisk + "\u00B4";
    public static final String letters = usie_naciski
            + "-ёйцукенгшзхфывапролджэячсмтьъбющиЁЙЦУКЕНГШЗХФЫВАПРОЛДЖЭЯЧСМТЬЪБЮЩИqwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM0123456789";

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
        UMALYJA[pravilny_nacisk] = pravilny_nacisk;
        UMALYJA['\''] = '\'';
        UMALYJA['-'] = '-';

        LITENORMALIZE['ё'] = 'е'; // ё -> е
        LITENORMALIZE['Ё'] = 'Е';
        // Націскі
        LITENORMALIZE[pravilny_nacisk] = pravilny_nacisk;
        LITENORMALIZE['\u00B4'] = pravilny_nacisk;
        LITENORMALIZE['\u0301'] = pravilny_nacisk; // combined accent
        LITENORMALIZE['-'] = '-';
        // пошук
        LITENORMALIZE['?'] = '?';
        LITENORMALIZE['*'] = '*';

        SUPERNORMALIZE['ё'] = 'е'; // ё -> е
        SUPERNORMALIZE['Ё'] = 'Е';
        SUPERNORMALIZE['-'] = '-';
        SUPERNORMALIZE['?'] = '?';
        SUPERNORMALIZE['*'] = '*';
    }

    @Override
    public String lightNormalized(CharSequence word) {
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
    public boolean isApostraf(char c) {
        return false;
    }

    @Override
    public boolean isLetter(char c) {
        return letters.indexOf(c) >= 0;
    }

    /**
     * Параўноўвае слова ў базе(ці ўведзенае карыстальнікам слова) з словам у
     * тэксце. Тут правяраюцца ўсе "несупярэчнасці": націскі, вялікія літары,
     * апострафы, Г выбухная, Ў напачатку слова.
     * 
     * Націскі могуць быць альбо не быць як у базе, так і ў тэксце.
     */
    @Override
    public boolean equals(String dbWord, String anyWord) {
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
                if (cDb == pravilny_nacisk) {
                    stressWasMissedInWord = 1;
                    continue;
                } else if (cAny == pravilny_nacisk) {
                    stressWasMissedInDb = 1;
                    continue;
                }
                return false;
            }
            if (cAny == 0 || cDb == 0) {
                return false;
            }
            // першы сімвал - можа вялікі ?
            boolean vialikiDb = CIVIALIKIJA[cDb];
            boolean vialikiAny = CIVIALIKIJA[cAny];
            if (vialikiDb && !vialikiAny) {
                return false;
            }
            cDb = UMALYJA[cDb];
            cAny = UMALYJA[cAny];
            if (cDb == pravilny_nacisk && cAny == pravilny_nacisk) {
                stressWasEquals = 1;
                continue;
            } else if (cDb == pravilny_nacisk) {
                stressWasMissedInWord = 1;
                iAny--;
                continue;
            } else if (cAny == pravilny_nacisk) {
                stressWasMissedInDb = 1;
                iDb--;
                continue;
            }
            if (cDb != cAny) {
                return false;
            }
        }
    }
}
