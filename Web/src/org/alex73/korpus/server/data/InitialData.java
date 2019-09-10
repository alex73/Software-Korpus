package org.alex73.korpus.server.data;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class InitialData {
    public List<String> authors;
    public List<String> volumes;
    public List<StatLine> statKorpus;
    public List<StatLine> statOther;
    public List<String> styleGenresParts;
    public Map<String,List<String>> styleGenres;
    public GrammarInitial grammar;
    
    public static class StatLine {
        public String name;
        public int texts;
        public int words;
    }
}
