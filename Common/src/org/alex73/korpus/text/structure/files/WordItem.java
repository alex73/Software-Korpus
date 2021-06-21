package org.alex73.korpus.text.structure.files;

import org.alex73.korpus.text.structure.corpus.Word.OtherType;

public class WordItem implements ITextLineElement {
    /** Зыходнае слова ў тэксце з мінімальным прывядзеннем да стандартнага запісу: выпраўленне апострафаў. Мусіць абавязкова быць. */
    public String lightNormalized;
    /** Зыходнае слова ў тэксце, выпраўленае карыстальнікам для пошуку ў базе, накшталт "мн-о-о-о-га"->"многа". Толькі калі карыстальнік абраў. */
    public String normalized;

    /** Аўтаматычна знойдзеныя лемы з базы, праз ';' */
    public String lemmas;
    /** Аўтаматычна знойдзеныя тэгі з базы, праз ';' */
    public String tags;

    /** Адна абраная лема пры зняцці аманіміі. Толькі калі карыстальнік абраў. */
    public String manualLemma;
    /** Адзін абраны тэг пры зняцці аманіміі. Толькі калі карыстальнік абраў. */
    public String manualTag;
    /** Нестандартны тып слова. Толькі калі карыстальнік абраў. */
    public OtherType type;

    public WordItem() {
    }

    public WordItem(String text) {
        lightNormalized = text;
    }

    @Override
    public String getText() {
        return lightNormalized;
    }
}
