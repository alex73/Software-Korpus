package org.alex73.korpus.server.data;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ResultText implements Serializable {
    public WordResult[][] words; // paragraph is array of sentences, i.e. of array of words
}
