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

import java.util.ArrayList;
import java.util.List;

public class BelarusianTags {
    public static final String NO_GROUP_ITEM = "не ўжываецца";
    public static final String HALOSNYJA = "ёуеыаоэяіюЁУЕЫАОЭЯІЮ";
    public static final String USUALLY_STRESSED = "ёоЁО";

    private static BelarusianTags INSTANCE = new BelarusianTags();

    public static BelarusianTags getInstance() {
        return INSTANCE;
    }

    private TagLetter root;

    public static void main(String[] a) {
        INSTANCE.isValid("VTPN1PG", "err");
    }

    private BelarusianTags() {
        root = new TagLetter();

        nazounik(root);
        licebnik(root);
        zajmiennik(root);
        prymietnik(root);
        dziejaslou(root);
        dziejeprymietnik(root);
        pryslouje(root);
        zlucnik(root);
        prynazounik(root);
        cascica(root);
        vyklicnik(root);
        pabocnaje(root);
        predykatyu(root);
        znaki(root);
    }

    public boolean isValid(String code, String w) {
        TagLetter tags = root;
        for (char c : code.toCharArray()) {
            if (c == 'x') { //TODO
                if (tags.letters.isEmpty()) {
                    if (w != null) {
                        System.out.println(code + " " + w + " - зашмат літараў у кодзе");
                    }
                    return false;
                }
                TagLetter first = tags.letters.get(0).nextLetters;
                for (TagLetter.OneLetterInfo li : tags.letters) {
                    if (li.nextLetters != first) {
                        if (w != null) {
                            System.out.println(code + " " + w + " - незразумелы шлях раскадаваньня");
                        }
                        return false;
                    }
                }
                tags = first;
            } else {
                tags = tags.next(c);
                if (tags == null) {
                    if (w != null) {
                        System.out.println(code + " " + w + " - невядомая літара ў кодзе");
                    }
                    return false;
                }
            }
        }
        if (!tags.letters.isEmpty()) {
            if (w != null) {
                System.out.println(code + " " + w + " - замалы код");
            }
            return false;
        }
        return true;
    }

    public TagLetter getRoot() {
        return root;
    }

    public TagLetter getNextAfter(String codeBegin) {
        TagLetter tags = root;
        for (char c : codeBegin.toCharArray()) {
            tags = tags.next(c);
            if (tags == null) {
                throw new RuntimeException("Error code: " + codeBegin);
            }
        }
        return tags;
    }

    public List<String> describe(String codeBegin) {
        List<String> result = new ArrayList<String>();
        TagLetter tags = root;
        for (char c : codeBegin.toCharArray()) {
            String descr = tags.getLetterDescription(c);
            if (descr == null) {
                throw new RuntimeException("Error code: " + codeBegin);
            }
            tags = tags.next(c);
            if (tags == null) {
                throw new RuntimeException("Error code: " + codeBegin);
            }
            result.add(descr);
        }
        return result;
    }

    private void nazounik(TagLetter t) {
        t = t.add("Часціна => N:Назоўнік");
        t = t.add("Уласнасць => C:агульны;P:уласны");
        t = t.add("Адушаўлёнасць => A:адушаўлёны;I:неадушаўлёны;X:???????");
        t = t.add("Асабовасць => P:асабовы;I:неасабовы;X:???????");
        t = t.add("Скарот => B:скарот;N:нескарот");

        TagLetter ns = t.add("Род => M:мужчынскі;F:жаночы;N:ніякі;P:множны лік;C:агульны;X:???????");
        ns = ns.add("Скланенне => 1:1 скланенне;2:2 скланенне;3:3 скланенне;0:нескланяльны;4:рознаскланяльны;6:змешаны;X:???????");
        ns = ns.add("Склон => N:назоўны;G:родны;D:давальны;A:вінавальны;I:творны;L:месны;V:клічны");
        ns = ns.add("Лік => S:адзіночны;P:множны");

        TagLetter s = t.add("Субстантываванасць => S:субстантываваны");
        s = s.add("Скланенне => 1:1 скланенне;2:2 скланенне;3:3 скланенне;0:нескланяльны;4:рознаскланяльны;5:ад’ектыўнае;6:змешаны");
        s = s.add("Род => M:мужчынскі;F:жаночы;N:ніякі;P:множны лік;X:???????");
        s = s.add("Склон => N:назоўны;G:родны;D:давальны;A:вінавальны;I:творны;L:месны;V:клічны");
        s = s.add("Лік => S:адзіночны;P:множны");
    }

    private void licebnik(TagLetter t) {
        t = t.add("Часціна => M:Лічэбнік");
        t = t.add("Словазмяненне => N:як у назоўніка;A:як у прыметніка;X:???????");

        t = t.add("Значэнне => C:колькасны;O:парадкавы;K:зборны;F:дробавы");
        t = t.add("Форма => S:просты;C:складаны");
        t = t.add("Род => M:мужчынскі;F:жаночы;N:ніякі;X:???????");
        t = t.add("Склон => N:назоўны;G:родны;D:давальны;A:вінавальны;I:творны;L:месны;V:клічны;X:???????");
        t = t.add("Лік => S:адзіночны;P:множны;X:???????");
    }

    private void zajmiennik(TagLetter t) {
        t = t.add("Часціна => S:Займеннік");
        t = t.add("Словазмяненне => N:як у назоўніка;A:як у прыметніка");
        t = t.add("Разрад => P:асабовы;R:зваротны;S:прыналежны;D:указальны;E:азначальны;L:пытальна–адносны;N:адмоўны;F:няпэўны");
        t = t.add("Асоба => 1:першая;2:другая;3:трэцяя;0:безасабовы;X:???????");
        t = t.add("Род => M:мужчынскі;F:жаночы;N:ніякі;X:???????");
        t = t.add("Склон => N:назоўны;G:родны;D:давальны;A:вінавальны;I:творны;L:месны;V:клічны;X:???????");
        t = t.add("Лік => S:адзіночны;P:множны;X:???????");
    }

    private void prymietnik(TagLetter t) {
        t = t.add("Часціна => A:Прыметнік");
        t.add("Тып => 0:нескланяльны");
        t = t.add("Тып => Q:якасны;R:адносны;P:прыналежны;X:???????");

        t = t.add("Ступень параўнання => P:станоўчая;C:вышэйшая;S:найвышэйшая");
        t = t.add("Род => M:мужчынскі;F:жаночы;N:ніякі;P:множны лік;X:???????");
        t = t.add("Склон => N:назоўны;G:родны;D:давальны;A:вінавальны;I:творны;L:месны;V:клічны");
        t = t.add("Лік => S:адзіночны;P:множны");
    }

    private void dziejaslou(TagLetter t) {
        t = t.add("Часціна => V:Дзеяслоў");
        t = t.add("Пераходнасць => T:пераходны;I:непераходны;D:пераходны/непераходны;X:???????");
        t = t.add("Трыванне => P:закончанае;M:незакончанае");
        t = t.add("Зваротнасць => R:зваротны;N:незваротны");
        t = t.add("Спражэнне => 1:першае;2:другое;3:рознаспрагальны");
        TagLetter casR = t.add("Час => R:цяперашні");
        TagLetter casM = t.add("Час => P:мінулы");
        TagLetter casO = t.add("Час => F:будучы;Q:перадмінулы");
        TagLetter zah = t.add("Загадны лад => I:загадны лад");
        t.add("Інфінітыў => 0:Інфінітыў");
        t.add("Невядома => X:невядома").add("Невядома => X:невядома").add("Невядома => X:невядома").add("Невядома => X:невядома");

        TagLetter casRL = casR.add("Асоба => 1:першая;2:другая;3:трэцяя;0:безасабовы");
        casR.add("Дзеепрыслоўе => G:дзеепрыслоўе");
        TagLetter casML = casM.add("Асоба => 1:першая;2:другая;3:трэцяя;0:безасабовы;X:???????");
        casM.add("Дзеепрыслоўе => G:дзеепрыслоўе");
        TagLetter casOL = casO.add("Асоба => 1:першая;2:другая;3:трэцяя;0:безасабовы");
        casO.add("Дзеепрыслоўе => G:дзеепрыслоўе");
        zah = zah.add("Асоба => 1:першая;2:другая;3:трэцяя;0:безасабовы");
        zah.add("Дзеепрыслоўе => G:дзеепрыслоўе");

        casRL = casRL.add("Лік => S:адзіночны;P:множны");
        casML = casML.add("Лік => S:адзіночны;P:множны");
        casOL = casOL.add("Лік => S:адзіночны;P:множны");
        zah = zah.add("Лік => S:адзіночны;P:множны");
        casML = casML.add("Род => M:мужчынскі;F:жаночы;N:ніякі;X:???????");
    }

    private void dziejeprymietnik(TagLetter t) {
        t = t.add("Часціна => P:Дзеепрыметнік");
        t = t.add("Стан => A:незалежны;P:залежны");
        t = t.add("Час => R:цяперашні;P:мінулы");
        t = t.add("Трыванне => P:закончанае;M:незакончанае;D:закончанае/незакончанае;X:???????");
        t = t.add("Род => M:мужчынскі;F:жаночы;N:ніякі;P:множны лік;X:???????");
        t = t.add("Склон => N:назоўны;G:родны;D:давальны;A:вінавальны;I:творны;L:месны;V:клічны;H:???????");
        t = t.add("Лік => S:адзіночны;P:множны;X:???????");
    }

    private void pryslouje(TagLetter t) {
        t = t.add("Часціна => R:Прыслоўе");
        t = t.add("Утварэнне => N:ад назоўнікаў;A:прыметнікаў;M:лічэбнікаў;S:займеннікаў;G:дзеепрыслоўяў;V:дзеясловаў;E:часціц;I:прыназоўнікаў;X:???????");
        t = t.add("Ступень параўнання => P:станоўчая;C:вышэйшая;S:найвышэйшая");
    }

    private void zlucnik(TagLetter t) {
        t = t.add("Часціна => C:Злучнік");
        TagLetter s = t.add("Тып => S:падпарадкавальны");
        TagLetter k = t.add("Тып => K:злучальны");
        t.add("Тып => P:паясняльны");
        s.add("Падпарадкавальны => B:прычынны;C:часавы;D:умоўны;F:мэтавы;G:уступальны;H:параўнальны;K:следства;X:???????");
        k.add("Злучальны => A:спалучальны;E:супастаўляльны;O:пералічальна-размеркавальны;L:далучальны;U:градацыйны;X:???????");
    }

    private void prynazounik(TagLetter t) {
        t.add("Часціна => I:Прыназоўнік");
    }

    private void cascica(TagLetter t) {
        t.add("Часціна => E:Часціца");
    }

    private void vyklicnik(TagLetter t) {
        t.add("Часціна => Y:Выклічнік");
    }

    private void pabocnaje(TagLetter t) {
        t.add("Часціна => Z:Пабочнае слова");
    }

    private void predykatyu(TagLetter t) {
        t.add("Часціна => W:Прэдыкатыў");
    }

    private void znaki(TagLetter t) {
        t = t.add("Часціна => K:Знак прыпынку");
        t.add("Знак => K:,;R:.;E:!;T:...;2:двухкроп'е;3:трохкроп'е;A:пытальнік");
        TagLetter q = t.add("Знак => Q:двукоссе");
        q.add("Двукоссе => L:Левае;R:Правае;O:адчыняе;C:зачыняе");
        TagLetter m=t.add("Знак => M:мінус");
        m.add("Мінус => 1:1;2:2");
        TagLetter s=t.add("Знак => D:дужкі");
        s.add("Дужкі => O:Левая;C:Правая");
    }
}
