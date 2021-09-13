package org.alex73.korpus.text.structure.files;

public class InlineTag implements ITextLineElement {
    public String text;

    public InlineTag(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InlineTag) {
            InlineTag o = (InlineTag) obj;
            return text.equals(o.text);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return text;
    }
}
