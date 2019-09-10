package org.alex73.korpus.base;

/**
 * Разлічвае хэш слова:
 * 
 * - не ўлічвае вялікія/малыя літары
 * 
 * - апостраф улічвае толькі ўсярэдзіне слова
 * 
 * - націск неўлічваецца
 * 
 * - першая 'ў' мяняецца на 'у'
 * 
 * - 'г-выбуховае' ўлічваецца як звычайнае 'г'
 */
public class BelarusianWordHash {
    public static int hash(String word) {
        if (word == null) {
            return 0;
        }
        boolean isFirst = true;
        boolean wasApostraf = false;
        int result = 0;
        for (int i = 0; i < word.length(); i++) {
            char c = 0;
            switch (word.charAt(i)) {
            case '\'':
            case '\u02BC':
            case '\u2019':
                // Правільны апостраф - 02BC, але паўсюль ужываем лацінкавы
                if (!isFirst) {
                    wasApostraf = true;
                }
                continue;
            case '\u00B4':
            case '\u0301':
            case '+':
                // Націск: асобны знак - 00B4, спалучэньне з папярэдняй літарай - 0301
                continue;
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
                c = isFirst ? 'у' : 'ў';
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
            default:
                continue;
            }
            if (wasApostraf) {
                result = 31 * result + '\'';
            }
            result = 31 * result + c;
        }
        return result;
    }
}
