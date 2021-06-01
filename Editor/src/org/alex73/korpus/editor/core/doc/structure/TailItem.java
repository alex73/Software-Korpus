package org.alex73.korpus.editor.core.doc.structure;

public class TailItem implements ITextLineElement {
    public String text;

    public TailItem() {
    }

    public TailItem(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }
}
