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

package org.alex73.korpus.belarusian;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.alex73.korpus.belarusian.TagLetter.OneLetterInfo;

/**
 * Граматычныя пазнакі паказваюцца ў наступных месцах:
 * 1) Фільтар граматыкі для слова - checkboxes.
 * 2) Паказ граматычных табліц для слова.
 * 3) Паказ граматычных характарыстык аднаго слова.
 * 4) Граматычны слоўнік у "праектах у распрацоўцы".
 */
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
        INSTANCE.isValidParadigmTag("VTPN1", "err p");
        INSTANCE.isValidFormTag("VTPN1PG", "err f");
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
        abrev(root);
        castki(root);

        checkParadigmMarks();
    }

    /**
     * шукаем ва ўсіх тэгах count(latestInParadigm)==1
     */
    private void checkParadigmMarks() {
        checkParadigmMarks(root, "", 0);
    }

    private void checkParadigmMarks(TagLetter tl, String code, int pmCount) {
        if (tl.isLatestInParadigm()) {
            pmCount++;
        }
        if (tl.isFinish()) {
            if (pmCount != 1) {
                throw new RuntimeException("pmCount=" + pmCount + " for " + code);
            }
        } else {
            for (OneLetterInfo letterInfo : tl.letters) {
                checkParadigmMarks(letterInfo.nextLetters, code + letterInfo.letter, pmCount);
            }
        }
    }

    /**
     * Ці правільны тэг у парадыгме ? latestInParadigm==true
     */
    public boolean isValidParadigmTag(String code, String w) {
        TagLetter after = getTagLetterAfter(code, w);
        if (after == null) {
            return false;
        }
        if (!after.isLatestInParadigm()) {
            if (w != null) {
                System.out.println(code + " " + w + " - няправільны тэг парадыгмы");
            }
            return false;
        }
        return true;
    }

    public boolean isValidFormTag(String code, String w) {
        TagLetter after = getTagLetterAfter(code, w);
        if (after == null) {
            return false;
        }
        if (!after.isFinish()) {
            if (w != null) {
                System.out.println(code + " " + w + " - замалы код");
            }
            return false;
        }
        return true;
    }

    private TagLetter getTagLetterAfter(String code, String w) {
        TagLetter tags = root;
        for (char c : code.toCharArray()) {
            if (c == 'x') { // TODO
                if (tags.isFinish()) {
                    if (w != null) {
                        System.out.println(code + " " + w + " - зашмат літараў у кодзе");
                    }
                    return null;
                }
                TagLetter first = tags.letters.get(0).nextLetters;
                for (TagLetter.OneLetterInfo li : tags.letters) {
                    if (li.nextLetters != first) {
                        if (w != null) {
                            System.out.println(code + " " + w + " - незразумелы шлях раскадаваньня");
                        }
                        return null;
                    }
                }
                tags = first;
            } else {
                tags = tags.next(c);
                if (tags == null) {
                    if (w != null) {
                        System.out.println(code + " " + w + " - невядомая літара ў кодзе");
                    }
                    return null;
                }
            }
        }
        return tags;
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

    public List<String> describe(String codeBegin, Set<String> excludeGroups) {
        List<String> result = new ArrayList<String>();
        TagLetter tags = root;
        for (char c : codeBegin.toCharArray()) {
            OneLetterInfo info = tags.getLetterInfo(c);
            if (info == null) {
                throw new RuntimeException("Wrong tag: " + codeBegin);
            }
            tags = tags.next(c);
            if (tags == null) {
                throw new RuntimeException("Wrong tag: " + codeBegin);
            }
            if (excludeGroups == null || !excludeGroups.contains(info.groupName)) {
                result.add(info.description);
            }
        }
        return result;
    }

    public char getValueOfGroup(String code, String group) {
        TagLetter tags = root;
        for (char c : code.toCharArray()) {
            OneLetterInfo li = tags.getLetterInfo(c);
            if (li == null) {
                throw new RuntimeException("Wrong tag: " + code);
            }
            if (group.equals(li.groupName)) {
                return li.letter;
            }
            tags = tags.next(c);
            if (tags == null) {
                throw new RuntimeException("Wrong tag: " + code);
            }
        }
        return 0;
    }

    public String setValueOfGroup(String code, String group, char newValue) {
        TagLetter tags = root;
        for (int i=0;i<code.length();i++) {
            char c= code.charAt(i);
            OneLetterInfo li = tags.getLetterInfo(c);
            if (li == null) {
                throw new RuntimeException("Wrong tag: " + code);
            }
            if (group.equals(li.groupName)) {
                return code.substring(0, i) + newValue + code.substring(i + 1);
            }
            tags = tags.next(c);
            if (tags == null) {
                throw new RuntimeException("Wrong tag: " + code);
            }
        }
        return null;
    }

    private void nazounik(TagLetter t) {
        t = t.add("Часціна мовы => N:назоўнік");
        t.add("Новы=>+:новы").latestInParadigm();
        t = t.add("Уласнасць => C:агульны;P:уласны;X:-");
        t = t.add("Адушаўлёнасць => A:адушаўлёны;I:неадушаўлёны;X:-");
        t = t.add("Асабовасць => P:асабовы;I:неасабовы;X:-");
        t = t.add("Скарачэнне => B:скарачэнне;N:не скарачэнне;X:-");

        TagLetter z = t.add("Род => M:м.;F:ж.;N:н.;C:агульн.;X:-");
        z = z.add(
                "Скланенне => 1:1-е;2:2-е;3:3-е;0:нескл.;4:рознаскл.;6:змеш.;X:-")
                .latestInParadigm();
        z = z.add("Склон => N:Н.;G:Р.;D:Д.;A:В.;I:Т.;L:М.;V:Кл.");
        z = z.add("Лік => S:адз.;P:мн.");

        TagLetter p = t.add("Множналікавыя => P:множны лік");
        p = p.add("Скланенне => 0:нескл.;7:множналік.").latestInParadigm();
        p = p.add("Склон => N:Н.;G:Р.;D:Д.;A:В.;I:Т.;L:М.;V:Кл.");
        p = p.add("Лік => S:адз.;P:мн.");

        TagLetter su = t.add("Субстантываванасць => S:субстантываваны;U:субстантываваны множналікавы");
        su = su.add("Скланенне => 5:ад’ектыўнае").latestInParadigm();
        su = su.add("Род => M:м.;F:ж.;N:н.;P:адсутнасьць роду ў множным ліку;X:-");
        su = su.add("Склон => N:Н.;G:Р.;D:Д.;A:В.;I:Т.;L:М.;V:Кл.");
        su = su.add("Лік => S:адз.;P:мн.");
    }

    private void licebnik(TagLetter t) {
        TagLetter t0 = t.add("Часціна мовы => M:лічэбнік");
        t = t0.add("Словазмяненне => N:як у назоўніка;A:як у прыметніка;X:-");
        TagLetter t2=t0.add("Словазмяненне => 0:няма");
        t = t.add("Значэнне => C:колькасны;O:парадкавы;K:зборны;F:дробавы");
        t2 = t2.add("Значэнне => C:колькасны;O:парадкавы;K:зборны;F:дробавы");
        t = t.add("Форма => S:просты;C:складаны").latestInParadigm();
        t2 = t2.add("Форма => S:просты;C:складаны").latestInParadigm();

        TagLetter z = t.add("Род => M:м.;F:ж.;N:н.;P:няма;X:-");
        t2.add("Нескланяльны => 0:нескл.");
        z = z.add("Склон => N:Н.;G:Р.;D:Д.;A:В.;I:Т.;L:М.;X:-");
        z = z.add("Лік => S:адз.;P:мн.;X:-");
    }

    private void zajmiennik(TagLetter t) {
        t = t.add("Часціна мовы => S:займеннік");
        t = t.add("Словазмяненне => N:як у назоўніка;A:як у прыметніка;0:нязменны");
        t = t.add(
                "Разрад => P:асабовы;R:зваротны;S:прыналежны;D:указальны;E:азначальны;L:пытальна-адносны;N:адмоўны;F:няпэўны");
        t = t.add("Асоба => 1:1-я асоба;2:2-я асоба;3:3-я асоба;0:безасабовы;X:-").latestInParadigm();

        TagLetter z = t.add("Род => M:м.;F:ж.;N:н.;X:-;0:-");
        t.add("Формы => 1:-");
        z = z.add("Склон => N:Н.;G:Р.;D:Д.;A:В.;I:Т.;L:М.;X:-");
        z = z.add("Лік => S:адз.;P:мн.;X:-");
    }

    private void prymietnik(TagLetter t) {
        t = t.add("Часціна мовы => A:прыметнік");
        t.add("Тып => 0:нескланяльны").latestInParadigm();
        t = t.add("Тып => Q:якасны;R:адносны;P:прыналежны;X:-");
        TagLetter a = t.add("Ступень параўнання => P:станоўчая;C:вышэйшая;S:найвышэйшая").latestInParadigm();

        a.add("Прыметнік у функцыі прыслоўя => R:прысл.");
        t = a.add("Род => M:м.;F:ж.;N:н.;P:мн. лік;X:-");
        t = t.add("Склон => N:Н.;G:Р.;D:Д.;A:В.;I:Т.;L:М.");
        t = t.add("Лік => S:адз.;P:мн.");
    }

    private void dziejaslou(TagLetter t) {
        t = t.add("Часціна мовы => V:дзеяслоў");
        t.add("Новы=>+:новы").latestInParadigm();
        t = t.add("Пераходнасць => T:пераходны;I:непераходны;D:пераходны/непераходны;X:-");
        t = t.add("Трыванне => P:закончанае трыванне;M:незакончанае трыванне;X:-");
        t = t.add("Зваротнасць => R:зваротны;N:незваротны");
        t = t.add("Спражэнне => 1:1-е спражэнне;2:2-е спражэнне;3:рознаспрагальны;X:-").latestInParadigm();

        TagLetter casR = t.add("Час => R:цяпер.");
        TagLetter casM = t.add("Час => P:пр.");
        TagLetter casO = t.add("Час => F:буд.;Q:перадпр.");
        TagLetter zah = t.add("Загадны лад => I:заг.");
        t.add("Інфінітыў => 0:інф.");
        t.add("Невядома => X:-").add("Невядома => X:-").add("Невядома => X:-")
                .add("Невядома => X:-");

        TagLetter casRL = casR.add("Асоба => 1:1-я;2:2-я;3:3-я;0:безас.");
        casR.add("Дзеепрыслоўе => G:дзеепрысл.");
        casM.add("Дзеепрыслоўе => G:дзеепрысл.");
        TagLetter casOL = casO.add("Асоба => 1:1-я;2:2-я;3:3-я;0:безас.");
        casO.add("Дзеепрыслоўе => G:дзеепрысл.");
        zah = zah.add("Асоба => 1:1-я;2:2-я;3:3-я;0:безас.");

        casRL = casRL.add("Лік => S:адз.;P:мн.");
        casOL = casOL.add("Лік => S:адз.;P:мн.");
        zah = zah.add("Лік => S:адз.;P:мн.");

        casM = casM.add("Род => M:м.;F:ж.;N:н.;X:-");
        casM = casM.add("Лік => S:адз.;P:мн.");
    }

    private void dziejeprymietnik(TagLetter t) {
        t = t.add("Часціна мовы => P:дзеепрыметнік");
        t = t.add("Стан => A:незалежны стан;P:залежны стан");
        t = t.add("Час => R:цяперашні час;P:прошлы час");
        TagLetter pt = t.add("Трыванне => P:закончанае трыванне;M:незакончанае трыванне;X:-")
                .latestInParadigm();

        t = pt.add("Род => M:м.;F:ж.;N:н.;P:мн..;X:-");
        t = t.add("Склон => N:Н.;G:Р.;D:Д.;A:В.;I:Т.;L:М.;H:-");
        t = t.add("Лік => S:адз.;P:мн.;X:-");
        pt.add("Кароткая форма => R:ж. і н.");
    }

    private void pryslouje(TagLetter t) {
        t = t.add("Часціна мовы => R:прыслоўе");
        t.add("Новы=>+:новы").latestInParadigm();
        t = t.add(
                "Спосаб утварэння => N:ад назоўніка;A:ад прыметніка;M:ад лічэбніка;S:ад займенніка;G:ад дзеепрыслоўя;V:ад дзеяслова;E:ад часціцы;I:ад прыназоўніка;X:-")
                .latestInParadigm();

        t = t.add("Ступень параўнання => P:станоўчая;C:вышэйшая;S:найвышэйшая");
    }

    private void zlucnik(TagLetter t) {
        t = t.add("Часціна мовы => C:злучнік");
        TagLetter s = t.add("Тып => S:падпарадкавальны");
        TagLetter k = t.add("Тып => K:злучальны");
        t.add("Тып => P:паясняльны").latestInParadigm();
        s.add("Падпарадкавальны => B:прычынны;C:часавы;D:умоўны;F:мэтавы;G:уступальны;H:параўнальны;K:следства;X:-")
                .latestInParadigm();
        k.add("Злучальны => A:спалучальны;E:супастаўляльны;O:пералічальна-размеркавальны;L:далучальны;U:градацыйны;X:-")
                .latestInParadigm();
    }

    private void prynazounik(TagLetter t) {
        t.add("Часціна мовы => I:прыназоўнік").latestInParadigm();
    }

    private void cascica(TagLetter t) {
        t.add("Часціна мовы => E:часціца").latestInParadigm();
    }

    private void vyklicnik(TagLetter t) {
        t.add("Часціна мовы => Y:выклічнік").latestInParadigm();
    }

    private void pabocnaje(TagLetter t) {
        t.add("Часціна мовы => Z:пабочнае слова").latestInParadigm();
    }

    private void predykatyu(TagLetter t) {
        t.add("Часціна мовы => W:прэдыкатыў").latestInParadigm();
    }

    private void abrev(TagLetter t) {
        t = t.add("Часціна мовы => K:абрэвіятуры").latestInParadigm();
    }

    private void castki(TagLetter t) {
        t = t.add("Часціна мовы => F:частка слова");
        t.add("Тып => P:прыстаўка;F:1-я састаўная частка складаных слоў;S:2-я састаўная частка складаных слоў")
                .latestInParadigm();
    }
}
