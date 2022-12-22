package org.alex73.korpus.languages;

import org.alex73.korpus.languages.belarusian.BelarusianTags;
import org.alex73.korpus.languages.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.languages.russian.RussianTags;
import org.alex73.korpus.languages.russian.RussianWordNormalizer;

public class LanguageFactory {

    private static ILanguage belarusian = new ILanguage() {
        static final BelarusianTags bt = new BelarusianTags();
        static final DBTagsFactory dbtf = new DBTagsFactory(bt);
        static final BelarusianWordNormalizer n = new BelarusianWordNormalizer();

        public ILanguage.IGrammarTags getTags() {
            return bt;
        }

        @Override
        public IDBTags getDbTags() {
            return dbtf;
        }

        public INormalizer getNormalizer() {
            return n;
        };
    };

    private static ILanguage russian = new ILanguage() {
        static final RussianTags ts = new RussianTags();
        static final DBTagsFactory dbtf = new DBTagsFactory(ts);
        static final RussianWordNormalizer n = new RussianWordNormalizer();

        public ILanguage.IGrammarTags getTags() {
            return ts;
        }

        @Override
        public IDBTags getDbTags() {
            return dbtf;
        }

        public INormalizer getNormalizer() {
            return n;
        };
    };

    public static ILanguage get(String lang) {
        switch (lang) {
        case "bel":
            return belarusian;
        case "rus":
            return russian;
        default:
            throw new RuntimeException("Language not defined: " + lang);
        }
    }
}
