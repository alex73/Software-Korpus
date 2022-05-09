package org.alex73.korpus.compiler;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.structure.corpus.Paragraph;

public class ProcessTexts extends BaseParallelProcessor {
    public static final boolean writeToLucene = true;

    private static ProcessTexts instance;

    private final StaticGrammarFiller2 grFiller;
    private final ProcessPrepareLucene lucene;
    private final ProcessStat stat;
    private static final AtomicInteger counter = new AtomicInteger();
    private final int totalCount;

    public ProcessTexts(StaticGrammarFiller2 grFiller, ProcessPrepareLucene lucene, ProcessStat stat, int textsCount) throws Exception {
        super(4, 4);
        this.grFiller = grFiller;
        this.lucene = lucene;
        this.stat = stat;

        totalCount = textsCount;
        instance = this;
    }

    public static void process(TextInfo textInfo, List<Paragraph> content) {
        if (textInfo.sourceFilePath == null) {
            throw new RuntimeException("sourceFilePath нявызначаны");
        }
        if (textInfo.subcorpus == null) {
            throw new RuntimeException("subcorpus нявызначаны ў " + textInfo.sourceFilePath);
        }
        if (content.contains(null)) {
            throw new RuntimeException("content утрымлівае пустыя параграфы");
        }

        instance.run(() -> {
            counter.incrementAndGet();
            //System.out.println("Process: " + textInfo.sourceFilePath);
            Collections.shuffle(content);
            instance.grFiller.fill(content);
            instance.stat.process(textInfo, content);
            if (writeToLucene) {
                instance.lucene.process(textInfo, content);
            }
        });
    }

    @Override
    protected void dumpStat(StringBuilder o) {
        super.dumpStat(o);
        o.append(", texts processed: " + counter.get() + "/" + totalCount);
    }
}
