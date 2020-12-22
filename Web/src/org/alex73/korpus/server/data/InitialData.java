package org.alex73.korpus.server.data;

import java.util.List;
import java.util.Map;

import org.alex73.korpus.base.DBTagsGroups.KeyValue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class InitialData {
    public List<String> authors;
    public List<String> sources;
    public List<KeyValue> subcorpuses;
    public List<String> styleGenresParts;
    public Map<String,List<String>> styleGenres;
    public GrammarInitial grammar;
}
