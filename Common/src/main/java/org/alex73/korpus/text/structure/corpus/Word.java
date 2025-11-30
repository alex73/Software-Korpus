package org.alex73.korpus.text.structure.corpus;

import java.io.Serializable;

/**
 * Class for store word inside binary paragraph in corpus.
 */
@SuppressWarnings("serial")
public class Word implements Serializable {
    public enum OtherType {
        OTHER_LANGUAGE, NUMBER, TRASIANKA, DYJALEKT, PAZNAKA
    }

    /**
     * Зыходнае слова ў тэксце. Падаецца для паказу ў корпусе.
     */
    public String word;
    /**
     * Знакі і прагалы, якія стаяць пасля слова. Падаецца для паказу ў корпусе.
     */
    public String tail;

    public OtherType type;

    /**
     * Слова, на якое выправілі слова ў тэксце. Накшталт : а-а-ага => ага. Калі не
     * выпраўлялі, гэтае поле - пустое. Калі выпраўлялі, то гэтае поле мусіць
     * выкарыстоўвацца для wordNormalized і wordWriteVariant.
     */
    // not supported: public String wordReplaced;

    /**
     * Слова для звычайнага пошуку, і базавае слова для пошуку "з варыянтамі
     * напісання".
     */
    transient public String wordNormalized, wordSuperNormalized, wordZnakNormalized;
    /**
     * Тэгі для слова згодна звычайнаму пошуку, і тэгі для пошуку "з варыянтамі
     * напісання".
     */
    transient public String tagsNormalized, tagsVariants;
}
