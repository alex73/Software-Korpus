package org.alex73.korpus.compiler;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.belarusian.BelarusianWordNormalizer;
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

    @Override
    public void accept(MessageParsedText text) {
        run(() -> {
            BinaryParagraphWriter pwr = new BinaryParagraphWriter();
            Set<String> values = new HashSet<>();
            Set<String> dbGrammarTags = new HashSet<>();
            Set<String> lemmas = new HashSet<>();

            MessageLuceneWrite out = new MessageLuceneWrite();
            out.textInfo = text.textInfo;
            out.paragraphs = new MessageLuceneWrite.LuceneParagraph[text.paragraphs.size()];
            for (int i = 0; i < text.paragraphs.size(); i++) {
                Paragraph p = text.paragraphs.get(i);
                values.clear();
                dbGrammarTags.clear();
                lemmas.clear();
                for (Sentence se : p.sentences) {
                    for (Word w : se.words) {
                        String wc = BelarusianWordNormalizer.superNormalized(w.normalized);
                        values.add(wc);
                        if (w.tags != null && !w.tags.isEmpty()) {
                            for (String t : w.tags.split(";")) {
                                dbGrammarTags.add(DBTagsGroups.getDBTagString(t));
                            }
                        }
                        if (w.lemmas != null && !w.lemmas.isEmpty()) {
                            for (String t : w.lemmas.split(";")) {
                                lemmas.add(t);
                            }
                        }
                    }
                }

                MessageLuceneWrite.LuceneParagraph po = new MessageLuceneWrite.LuceneParagraph();
                po.page = p.page;
                po.xml = pwr.write(p);
                po.values = values.toArray(STRING_ARRAY);
                po.lemmas = lemmas.toArray(STRING_ARRAY);
                po.dbGrammarTags = dbGrammarTags.toArray(STRING_ARRAY);
                po.dbGrammarTags = dbGrammarTags.toArray(STRING_ARRAY);
                out.paragraphs[i] = po;
            }
            lucene.accept(out);
        });
    }
}
