package org.alex73.korpus.text.structure.corpus;

import java.io.Serializable;

/**
 * Class for store paragraph inside binary paragraph in corpus. For poems, one
 * paragraph is one stanza.
 */
@SuppressWarnings("serial")
public class Paragraph implements Serializable {
    public String lang;
    public String page;
    public String audioPreview;
    public Sentence[] sentences;
}
