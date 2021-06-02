package org.alex73.korpus.text.structure.corpus;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Paragraph implements Serializable {
    public int page;
    public Sentence[] sentences;
}
