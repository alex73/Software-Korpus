package org.alex73.korpus.text.structure.files;

import org.apache.commons.lang.StringUtils;

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
            return StringUtils.equals(o.text, text);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return text;
    }
}
