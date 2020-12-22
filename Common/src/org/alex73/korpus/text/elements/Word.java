package org.alex73.korpus.text.elements;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Word implements Serializable {
    public String lightNormalized;
    public String tail; // tail chars
    public String lemmas; // like 'word1;word2'
    public String tags; // like 'V12;N34'. We can't store db tags here because UI should display data by usual tags
}
