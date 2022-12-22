package org.alex73.korpus.languages;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alex73.korpus.languages.DBTagsFactory.DBTagsGroup;
import org.alex73.korpus.languages.DBTagsFactory.KeyValue;

public interface ILanguage {
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
        String lightNormalized(CharSequence word);

        String superNormalized(String word);

        int hash(String word);

        boolean isApostraf(char c);

        boolean isLetter(char c);

        /**
         * Параўноўвае слова ў базе(ці ўведзенае карыстальнікам слова) з словам у
         * тэксце. Тут правяраюцца ўсе "несупярэчнасці": націскі, вялікія літары,
         * апострафы, Г выбухная, Ў напачатку слова.
         * 
         * Націскі могуць быць альбо не быць як у базе, так і ў тэксце.
         */
        boolean equals(String dbWord, String anyWord);
    }
}
