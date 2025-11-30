package org.alex73.korpus.compiler;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.base.TextInfo.Subtext;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.server.text.BinaryParagraphWriter;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.text.structure.corpus.Word;

public class Step4PrepareLucene {
    public static MessageLuceneWrite run(MessageParsedText text) throws Exception {
        BinaryParagraphWriter pwr = new BinaryParagraphWriter();

        Set<String> values = new HashSet<>();
        Set<String> dbGrammarTags = new HashSet<>();

        MessageLuceneWrite out = new MessageLuceneWrite();
        out.textInfo = text.textInfo;
        for (TextInfo.Subtext st : text.textInfo.subtexts) {
            if (st.label == null) {
                throw new RuntimeException("No text label for " + text.textInfo.sourceFilePath);
            }
        }
        out.paragraphs = new MessageLuceneWrite.LuceneParagraph[text.getParagraphsCount()];
        for (int i = 0; i < out.paragraphs.length; i++) {
            MessageLuceneWrite.LuceneParagraph po = new MessageLuceneWrite.LuceneParagraph();
            out.paragraphs[i] = po;
            po.xml = pwr.write(getParagraphsByIndex(text, i));
            for (int l = 0; l < text.languages.length; l++) {
                ILanguage lang = LanguageFactory.get(text.languages[l].lang);
                values.clear();
                dbGrammarTags.clear();
                Paragraph p = text.languages[l].paragraphs[i];
                for (Sentence se : p.sentences) {
                    for (Word w : se.words) {
                        String wc = w.wordSuperNormalized;
                        if (wc != null) {
                            values.add(wc);
                        }
                        if (w.tagsVariants != null) {
                            for (String t : w.tagsVariants.split(";")) {
                                dbGrammarTags.add(lang.getDbTags().getDBTagString(t));
                            }
                        }
                    }
                }

                MessageLuceneWrite.LuceneParagraphLang pol = new MessageLuceneWrite.LuceneParagraphLang();
                pol.values = values.toArray(new String[0]);
                pol.dbGrammarTags = dbGrammarTags.toArray(new String[0]);
                po.byLang.put(text.languages[l].lang, pol);
            }
        }

        return out;
    }

    private static Paragraph[] getParagraphsByIndex(MessageParsedText text, int pi) {
        Paragraph[] r = new Paragraph[text.languages.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = text.languages[i].paragraphs[pi];
        }
        return r;
    }

    private static String[] getAllSubtextLanguages(TextInfo text) {
        Set<String> r = new TreeSet<>();
        for (Subtext st : text.subtexts) {
            r.add(st.lang);
        }
        return r.toArray(new String[r.size()]);
    }
}
