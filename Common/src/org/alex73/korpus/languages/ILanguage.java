package org.alex73.korpus.languages;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alex73.korpus.languages.DBTagsFactory.DBTagsGroup;
import org.alex73.korpus.languages.DBTagsFactory.KeyValue;

public interface ILanguage {
    String getLanguage();

    IGrammarTags getTags();

    IDBTags getDbTags();

    INormalizer getNormalizer();

    interface IGrammarTags {
        TagLetter getRoot();

        List<String> describe(String codeBegin, Set<String> excludeGroups);

        char getValueOfGroup(String code, String group);

        TagLetter getNextAfter(String codeBegin);
    }

    interface IDBTags {
        String getDBTagString(String grammarTag);

        List<KeyValue> getWordTypes();

        Map<Character, DBTagsGroup> getTagGroupsByWordType();
    }

    interface INormalizer {
        static final String PRESERVE_NONE = "";
        static final String PRESERVE_VISUAL = "[]";
        static final String PRESERVE_WILDCARDS = "?*";
        static final String PRESERVE_REGEXP = ".?*[]{},+()|^-0123456789";

        /**
         * Выпраўляем толькі апострафы і злучкі, націскі адкідаем.
         */
        String znakNormalized(CharSequence word, String preserveChars);

        /**
         * Невялікія выпраўленні: адкідае націскі, змяняе апострафы і злучкі на
         * правільныя, ў напачатку -> у, й напачатку -> і, вялікія літары -> малыя, ґ ->
         * г.
         */
        String lightNormalized(CharSequence word, String preserveChars);

        /**
         * Максімальная нармалізацыя - lightNormalization і дадаткова адкідаем мяккія
         * знакі, канвертуем мяккія галосныя ў цвёрдыя.
         */
        String superNormalized(String word, String preserveChars);

        /**
         * Падлік хэшу (з максімальнай нармалізацыяй).
         */
        int hash(String word);

        boolean isApostraf(char c);

        boolean isLetter(char c);
    }
}
