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
    public String sourceLinkSuffix;
    public String previewLinkSuffix;
    public Sentence[] sentences;

    public String toPlainText() {
        if (sentences == null || sentences.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Sentence s : sentences) {
            if (s != null && s.words != null) {
                for (Word w : s.words) {
                    if (w.word != null) {
                        sb.append(w.word);
                    }
                    if (w.tail != null) {
                        sb.append(w.tail);
                    }
                }
            }
        }
        return sb.toString().trim();
    }
}
