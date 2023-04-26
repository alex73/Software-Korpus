package org.alex73.korpus.languages.belarusian;

import java.util.Map;

import org.alex73.korpus.languages.ILanguage;

public class BelarusianWordNormalizer implements ILanguage.INormalizer {
    public static final char pravilny_nacisk = '\u0301';
    public static final String usie_naciski = pravilny_nacisk + "\u00B4";
    public static final char pravilny_apostraf = '\u02BC';
    public static final String usie_apostrafy = pravilny_apostraf + "\'\u2019";
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
    public String znakNormalized(CharSequence word, String preserveChars) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (preserveChars.indexOf(c) >= 0) {
                str.append(c);
                continue;
            }
            c = c < ZNAKNORMALIZE.length ? ZNAKNORMALIZE[c] : 0;
            if (c > 0) {
                str.append(c);
            }
        }
        return str.toString();
    }

    @Override
    public String lightNormalized(CharSequence word, String preserveChars) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (preserveChars.indexOf(c) >= 0) {
                str.append(c);
                continue;
            }
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
    public String superNormalized(String word, String preserveChars) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (preserveChars.indexOf(c) >= 0) {
                str.append(c);
                continue;
            }
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
