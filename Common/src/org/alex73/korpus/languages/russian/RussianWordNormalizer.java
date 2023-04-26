package org.alex73.korpus.languages.russian;

import org.alex73.korpus.languages.belarusian.BelarusianWordNormalizer;

public class RussianWordNormalizer extends BelarusianWordNormalizer {
    private final String lettersRus = usie_naciski
            + "-ёйцукенгшзхфывапролджэячсмтьъбющиЁЙЦУКЕНГШЗХФЫВАПРОЛДЖЭЯЧСМТЬЪБЮЩИqwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM0123456789";

    public RussianWordNormalizer() {
        LITENORMALIZE['ё'] = 'е'; // ё -> е
        LITENORMALIZE['Ё'] = 'е';
        SUPERNORMALIZE['ё'] = 'е'; // ё -> е
        SUPERNORMALIZE['Ё'] = 'е';
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
                str.append(c);
            }
        }
        return str.toString();
    }

    @Override
    public boolean isApostraf(char c) {
        return false;
    }

    @Override
    public boolean isLetter(char c) {
        return lettersRus.indexOf(c) >= 0;
    }
}
