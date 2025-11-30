package org.alex73.korpus.text.structure.files;

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
