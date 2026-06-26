package org.alex73.korpus.compiler;

import org.alex73.korpus.base.StaticGrammarFiller2;

/**
 * Другі этап кампіляцыі корпуса.
 * Клас забяспечвае марфалагічны аналіз тэксту, запаўняючы аб'екты слоў
 * граматычнай інфармацыяй з выкарыстаннем StaticGrammarFiller2.
 */
public class Step2Grammar {
    private static StaticGrammarFiller2 grFiller;

    public static void init(StaticGrammarFiller2 filler) {
        grFiller = filler;
    }

    public static void run(MessageParsedText text) throws Exception {
        for (MessageParsedText.Language la : text.languages) {
            grFiller.fill(la.paragraphs, true);
        }
    }
}
