package org.alex73.korpus.compiler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.server.text.BinaryParagraphWriter;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.text.structure.corpus.Word;

public class ProcessPrepareLucene extends BaseParallelProcessor {
    static final String[] STRING_ARRAY = new String[0];
    private ProcessLuceneWriter lucene;

    public ProcessPrepareLucene(ProcessLuceneWriter lucene) throws Exception {
        super(8, 20);
        this.lucene = lucene;
    }

    public void process(TextInfo textInfo, List<Paragraph> content) {
        run(() -> {
            BinaryParagraphWriter pwr = new BinaryParagraphWriter();
            Set<String> values = new HashSet<>();
            Set<String> dbGrammarTags = new HashSet<>();
            Set<String> lemmas = new HashSet<>();
            for (Paragraph p : content) {
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
                byte[] pxml = pwr.write(p);
                lucene.process(textInfo, p.page, values.toArray(STRING_ARRAY), dbGrammarTags.toArray(STRING_ARRAY),
                        lemmas.toArray(STRING_ARRAY), pxml);
            }
        });
    }
}
