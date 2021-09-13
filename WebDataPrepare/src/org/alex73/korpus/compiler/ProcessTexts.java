package org.alex73.korpus.compiler;

import java.util.Collections;
import java.util.List;

import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.structure.corpus.Paragraph;

public class ProcessTexts extends BaseParallelProcessor {
    public static final boolean writeToLucene = true;

    private static ProcessTexts instance;

    private final StaticGrammarFiller2 grFiller;
    private final ProcessPrepareLucene lucene;
    private final ProcessStat stat;

    public ProcessTexts(StaticGrammarFiller2 grFiller, ProcessPrepareLucene lucene, ProcessStat stat) throws Exception {
        super(4, 20);
        this.grFiller = grFiller;
        this.lucene = lucene;
        this.stat = stat;

        instance = this;
    }

    public static void process(TextInfo textInfo, List<Paragraph> content) {
        if (textInfo.sourceFilePath == null) {
            throw new RuntimeException("sourceFilePath нявызначаны");
        }
        if (textInfo.subcorpus == null) {
            throw new RuntimeException("subcorpus нявызначаны ў " + textInfo.sourceFilePath);
        }
        if (textInfo.title == null) {
            throw new RuntimeException("title нявызначаны ў " + textInfo.sourceFilePath);
        }
        if (content.contains(null)) {
            throw new RuntimeException("content утрымлівае пустыя параграфы");
        }

        instance.run(() -> {
            Collections.shuffle(content);
            instance.grFiller.fillNonManual(content);
            instance.stat.process(textInfo, content);
            if (writeToLucene) {
                instance.lucene.process(textInfo, content);
            }
        });
    }
}
