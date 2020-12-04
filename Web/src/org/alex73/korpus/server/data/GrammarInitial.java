package org.alex73.korpus.server.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.base.DBTagsGroups.KeyValue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class GrammarInitial {
    public Map<Character, GrammarLetter> grammarTree;
    public List<KeyValue> grammarWordTypes;
    public Map<Character, DBTagsGroups> grammarWordTypesGroups;
    public Map<Character, Set<String>> skipGrammar;

    @JsonInclude(Include.NON_NULL)
    public static class GrammarLetter {
        public String name;
        public String desc;
        public Map<Character, GrammarLetter> ch;
    }
}
