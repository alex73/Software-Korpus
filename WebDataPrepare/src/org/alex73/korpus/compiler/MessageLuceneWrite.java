package org.alex73.korpus.compiler;

import org.alex73.korpus.base.TextInfo;

public class MessageLuceneWrite {
    public TextInfo textInfo;
    public LuceneParagraph[] paragraphs;

    public static class LuceneParagraph {
        public int page;
        public String[] values;
        public String[] dbGrammarTags;
        public String[] lemmas;
        public byte[] xml;
    }
}
