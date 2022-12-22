package org.alex73.korpus.compiler;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.structure.corpus.Paragraph;

public class MessageParsedText {
    public TextInfo textInfo;
    public Paragraph[][] paragraphs; // first index is paragraph index, second index - text index

    public MessageParsedText(int parallelCount) {
        textInfo = new TextInfo(parallelCount);
    }
}
