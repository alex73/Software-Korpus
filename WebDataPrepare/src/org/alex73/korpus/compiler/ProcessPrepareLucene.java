package org.alex73.korpus.compiler;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.base.TextInfo.Subtext;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.server.text.BinaryParagraphWriter;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.text.structure.corpus.Word;

public class ProcessPrepareLucene extends BaseParallelProcessor<MessageParsedText> {
    static final String[] STRING_ARRAY = new String[0];
    private Consumer<MessageLuceneWrite> lucene;

    public ProcessPrepareLucene(Consumer<MessageLuceneWrite> lucene) throws Exception {
        super(16, 40);
        this.lucene = lucene;
    }

    private String[] getAllSubtextLanguages(TextInfo text) {
        Set<String> r = new TreeSet<>();
        for (Subtext st : text.subtexts) {
            r.add(st.lang);
        }
        return r.toArray(new String[r.size()]);
    }

    @Override
    public void accept(MessageParsedText text) {
        run(() -> {
            BinaryParagraphWriter pwr = new BinaryParagraphWriter();

            String[] langs = getAllSubtextLanguages(text.textInfo);

            Set<String> values = new HashSet<>();
            Set<String> dbGrammarTags = new HashSet<>();

            MessageLuceneWrite out = new MessageLuceneWrite();
            out.textInfo = text.textInfo;
            for (TextInfo.Subtext st : text.textInfo.subtexts) {
                if (st.label == null) {
                    throw new RuntimeException("No text label for " + text.textInfo.sourceFilePath);
                }
            }
            out.paragraphs = new MessageLuceneWrite.LuceneParagraph[text.paragraphs.length];
            for (int i = 0; i < text.paragraphs.length; i++) {
                MessageLuceneWrite.LuceneParagraph po = new MessageLuceneWrite.LuceneParagraph();
                po.xml = pwr.write(text.paragraphs[i]);
                for (int l = 0; l < langs.length; l++) {
                    ILanguage lang = LanguageFactory.get(langs[l]);
                    values.clear();
                    dbGrammarTags.clear();
                    for (int j = 0; j < text.paragraphs[i].length; j++) {
                        if (!langs[l].equals(text.textInfo.subtexts[j].lang)) {
                            continue; // other language
                        }
                        Paragraph p = text.paragraphs[i][j];
                        for (Sentence se : p.sentences) {
                            for (Word w : se.words) {
                                String wc = w.wordSuperNormalized;
                                values.add(wc);
                                if (w.tagsVariants != null) {
                                    for (String t : w.tagsVariants.split(";")) {
                                        dbGrammarTags.add(lang.getDbTags().getDBTagString(t));
                                    }
                                }
                            }
                        }
                    }

                    MessageLuceneWrite.LuceneParagraphLang pol = new MessageLuceneWrite.LuceneParagraphLang();
                    pol.values = values.toArray(STRING_ARRAY);
                    pol.dbGrammarTags = dbGrammarTags.toArray(STRING_ARRAY);
                    po.byLang.put(langs[l], pol);
                }
                out.paragraphs[i] = po;
            }

            lucene.accept(out);
        });
    }
}
