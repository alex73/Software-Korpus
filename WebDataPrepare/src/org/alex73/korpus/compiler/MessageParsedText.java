package org.alex73.korpus.compiler;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.base.TextInfo.Subtext;
import org.alex73.korpus.text.structure.corpus.Paragraph;

public class MessageParsedText {
    public TextInfo textInfo;
    public Language[] languages;

    public MessageParsedText(int languagesCount) {
        textInfo = new TextInfo();
        textInfo.subtexts = new Subtext[languagesCount];
        languages = new Language[languagesCount];
    }

    public int getParagraphsCount() {
        for (int i = 1; i < languages.length; i++) {
            if (languages[0].paragraphs.length != languages[i].paragraphs.length) {
                throw new RuntimeException("Розная колькасць параграфаў для розных моў");
            }
        }
        return languages[0].paragraphs.length;
    }

    public static class Language {
        public String lang;
        public Paragraph[] paragraphs;
    }
}
