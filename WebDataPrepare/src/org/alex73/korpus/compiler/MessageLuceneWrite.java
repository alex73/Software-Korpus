package org.alex73.korpus.compiler;

import java.util.Map;
import java.util.TreeMap;

import org.alex73.korpus.base.TextInfo;

public class MessageLuceneWrite {
    public TextInfo textInfo;
    public LuceneParagraph[] paragraphs;

    public static class LuceneParagraph {
        public Map<String, LuceneParagraphLang> byLang = new TreeMap<>();
        public byte[] xml;
    }

    public static class LuceneParagraphLang {
        public String[] values;
        public String[] dbGrammarTags;
        public String[] lemmas;
    }
}
