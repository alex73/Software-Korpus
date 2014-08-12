/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2014 Aleś Bułojčyk (alex73mail@gmail.com)
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

package org.alex73.korpus.shared;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StyleGenres {

    public static final String[] KNOWN_GROUPS = new String[] { "мастацкі", "публіцыстычны", "навуковы",
            "афіцыйна-дзелавы" };
    public static final String[] KNOWN = new String[] { "іншыя", "афіцыйна-дзелавы/аб’ява",
            "афіцыйна-дзелавы/акт", "афіцыйна-дзелавы/аўтабіяграфія",
            "афіцыйна-дзелавы/афіцыйнае паведамленне", "афіцыйна-дзелавы/даведка",
            "афіцыйна-дзелавы/даверанасць", "афіцыйна-дзелавы/дамова", "афіцыйна-дзелавы/загад",
            "афіцыйна-дзелавы/закон", "афіцыйна-дзелавы/заява", "афіцыйна-дзелавы/інструкцыя",
            "афіцыйна-дзелавы/летапіс", "афіцыйна-дзелавы/пратакол", "афіцыйна-дзелавы/расклад",
            "афіцыйна-дзелавы/справаздача", "афіцыйна-дзелавы/статут", "мастацкі/апавяданне",
            "мастацкі/аповесць", "мастацкі/балада", "мастацкі/басня", "мастацкі/верш", "мастацкі/казка",
            "мастацкі/ода", "мастацкі/паэма", "мастацкі/п’еса", "мастацкі/раман", "мастацкі/сюжэт",
            "навуковы/анатацыя", "навуковы/даклад", "навуковы/дысертацыя", "навуковы/лекцыя",
            "навуковы/манаграфія", "навуковы/навуковы артыкул", "навуковы/навуковы выступ",
            "навуковы/навучальнае паведамленне", "навуковы/падручнік", "навуковы/папулярны артыкул",
            "навуковы/рэферат", "навуковы/рэцэнзія", "навуковы/тэзісы", "публіцыстычны/аратарская прамова",
            "публіцыстычны/артыкул", "публіцыстычны/выступ", "публіцыстычны/газетны артыкул",
            "публіцыстычны/дакументальная аповесць", "публіцыстычны/зварот", "публіцыстычны/інтэрвію",
            "публіцыстычны/нарыс", "публіцыстычны/нататка", "публіцыстычны/памфлет", "публіцыстычны/пахвала",
            "публіцыстычны/прадмова", "публіцыстычны/пуцявыя замалёўкі", "публіцыстычны/рэпартаж",
            "публіцыстычны/фельетон", "публіцыстычны/эсэ" };
    public static final String KNOWN_OTHER = "іншыя";

    public static final Set<String> KNOWN_SET;
    static {
        KNOWN_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(KNOWN)));
    }

    /**
     * Create code for URL.
     */
    public static String produceCode(List<String> selected) {
        selected.retainAll(KNOWN_SET);
        if (selected.size() == KNOWN_SET.size()) {
            return "";
        }

        StringBuilder o = new StringBuilder();
        // можа "акрамя" ?
        if (selected.size() >= KNOWN_SET.size() - 10 && !selected.contains(KNOWN_OTHER)) {
            // не пазначана менш за 10 жанраў і пазначаны "іншыя"
            Set<String> noselectedSet = new HashSet<>(KNOWN_SET);
            noselectedSet.removeAll(selected);
            o.append(";-");
            for (String s : noselectedSet) {
                o.append(';').append(s);
            }
        } else {
            for (String s : selected) {
                o.append(';').append(s);
            }
        }
        return o.substring(1);
    }

    /**
     * Converts code from URL into list of selected style/genres.
     */
    public static void restoreCode(String code, List<String> selected) {
        selected.clear();
        if (code == null || code.trim().isEmpty()) {
            return;
        }

        String[] codes = code.split(";");
        if ("-".equals(codes[0])) {
            selected.addAll(KNOWN_SET);
            for (int i = 1; i < codes.length; i++) {
                selected.remove(codes[i]);
            }
        } else {
            for (String s : codes) {
                selected.add(s);
            }
            selected.retainAll(KNOWN_SET);
        }
    }

    /**
     * Create user-friendly text of selected style/genres.
     */
    public static String getSelectedName(List<String> selected) {
        selected.retainAll(KNOWN_SET);

        StringBuilder o = new StringBuilder();
        if (selected.isEmpty() || selected.size() == KNOWN_SET.size()) {
            o.append("Усе");
        } else if (selected.size() >= KNOWN_SET.size() - 10) {
            Set<String> noselectedSet = new HashSet<>(KNOWN_SET);
            noselectedSet.removeAll(selected);
            for (String s : noselectedSet) {
                o.append(", ").append(s);
            }
            o.replace(0, 2, "Усе, акрамя: ");
        } else {
            for (String s : selected) {
                o.append(", ").append(s);
            }
            o.replace(0, 2, "");
        }
        return o.toString();
    }
}
