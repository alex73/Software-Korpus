package org.alex73.korpus.shared.dto;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class WordRequest implements Serializable {
    public String word;
    public boolean allForms;
    public String grammar;
    public List<String> lemmas;

    public boolean isWildcardWord() {
        return word.indexOf('*') >= 0 || word.indexOf('?') >= 0;
    }
}
