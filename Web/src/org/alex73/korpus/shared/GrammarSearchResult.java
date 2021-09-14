package org.alex73.korpus.shared;

import java.util.ArrayList;
import java.util.List;

public class GrammarSearchResult {
    public String error;
    public boolean hasDuplicateParadigms;
    public List<LemmaInfo> output = new ArrayList<>();
    public boolean hasMultiformResult;
}
