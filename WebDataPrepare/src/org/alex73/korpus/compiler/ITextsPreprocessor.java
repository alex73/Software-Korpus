package org.alex73.korpus.compiler;

import java.util.Comparator;
import java.util.Random;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.structure.corpus.Paragraph;

public interface ITextsPreprocessor {
    Comparator<TextInfo> getTextsComparator();

    void preprocess(MessageParsedText text);

    void constructTextPassport(TextInfo textInfo, TextInfo.Subtext subText);

    static void shuffle(Paragraph[][] list) {
        Random random = new Random();
        for (int i = list[0].length; i > 1; i--) {
            swap(list, i - 1, random.nextInt(i));
        }
    }

    private static void swap(Paragraph[][] list, int i, int j) {
        Paragraph[] temp = list[i];
        list[i] = list[j];
        list[j] = temp;
    }
}
