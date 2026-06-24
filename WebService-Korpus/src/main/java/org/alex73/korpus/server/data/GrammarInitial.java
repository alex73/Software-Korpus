package org.alex73.korpus.server.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alex73.korpus.languages.DBTagsFactory.DBTagsGroup;
import org.alex73.korpus.languages.DBTagsFactory.KeyValue;

public class GrammarInitial {
    public Map<Character, GrammarLetter> grammarTree;
    public List<KeyValue> grammarWordTypes;
    public Map<Character, DBTagsGroup> grammarWordTypesGroups;
    public Map<Character, Set<String>> skipGrammar;
    public List<GrammarDict> slouniki;
    public List<Stat> stat;

    public static class Stat {
        public String title;
        public int paradigmCount;
        public int formCount;
    }

    public static class GrammarLetter {
        public String name;
        public String desc;
        public Map<Character, GrammarLetter> ch;
    }

    public static class GrammarDict {
        public String name;
        public String desc;
    }
}
