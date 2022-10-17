package org.alex73.korpus.compiler;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.alex73.korpus.base.StaticGrammarFiller2;

public class ProcessTexts extends BaseParallelProcessor<MessageParsedText> {
    public static final boolean writeToLucene = true;

    private final StaticGrammarFiller2 grFiller;
    private final Consumer<MessageParsedText> lucene;
    private final ProcessStat stat;
    private static final AtomicInteger counter = new AtomicInteger();
    private final int totalCount;

    public ProcessTexts(StaticGrammarFiller2 grFiller, Consumer<MessageParsedText> lucene, ProcessStat stat, int textsCount) throws Exception {
        super(8, 16);
        this.grFiller = grFiller;
        this.lucene = lucene;
        this.stat = stat;

        totalCount = textsCount;
    }

    @Override
    public void accept(MessageParsedText text) {
        if (text.textInfo.sourceFilePath == null) {
            throw new RuntimeException("sourceFilePath нявызначаны");
        }
        if (text.textInfo.subcorpus == null) {
            throw new RuntimeException("subcorpus нявызначаны ў " + text.textInfo.sourceFilePath);
        }
        if (text.paragraphs.contains(null)) {
            throw new RuntimeException("content утрымлівае пустыя параграфы");
        }

        run(() -> {
            counter.incrementAndGet();
            // System.out.println("Process: " + textInfo.sourceFilePath);
            Collections.shuffle(text.paragraphs);
            grFiller.fill(text.paragraphs);
            stat.accept(text);
            for (int i = 0; i < text.paragraphs.size(); i += 500) {
                MessageParsedText portion = new MessageParsedText();
                portion.textInfo = text.textInfo;
                portion.paragraphs = text.paragraphs.subList(i, Math.min(text.paragraphs.size(), i + 500));
                lucene.accept(portion);
            }
        });
    }

    @Override
    protected void dumpStat(StringBuilder o) {
        super.dumpStat(o);
        o.append(", texts processed: " + counter.get() + "/" + totalCount);
    }
}
