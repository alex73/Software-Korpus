package org.alex73.korpus.languages.russian;

import java.util.List;
import java.util.Set;

import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.TagLetter;

public class RussianTags implements ILanguage.IGrammarTags {

    private final TagLetter root = new TagLetter();

    @Override
    public TagLetter getRoot() {
        return root;
    }

    @Override
    public List<String> describe(String codeBegin, Set<String> excludeGroups) {
        return null;
    }

    @Override
    public char getValueOfGroup(String code, String group) {
        return 0;
    }

    @Override
    public TagLetter getNextAfter(String codeBegin) {
        return null;
    }
}
