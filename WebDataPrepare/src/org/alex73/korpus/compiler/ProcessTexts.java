package org.alex73.korpus.compiler;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.alex73.korpus.base.StaticGrammarFiller2;

public class ProcessTexts extends BaseParallelProcessor<MessageParsedText> {
    public static final boolean writeToLucene = true;

    public static ITextsPreprocessor preprocessor;

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
            throw new RuntimeException("sourceFilePath not defined");
        }
        if (text.textInfo.subcorpus == null) {
            throw new RuntimeException("subcorpus not defined in the " + text.textInfo.sourceFilePath);
        }

        run(() -> {
            counter.incrementAndGet();
            // System.out.println("Process: " + textInfo.sourceFilePath);
            preprocessor.preprocess(text);
            for (int v = 0; v < text.paragraphs.length; v++) {
                for (int i = 0; i < text.textInfo.subtexts.length; i++) {
                    text.paragraphs[v][i].lang = text.textInfo.subtexts[i].lang;
                }
                grFiller.fill(Arrays.asList(text.paragraphs[v]));
            }
            stat.accept(text);
            for (int i = 0; i < text.paragraphs[0].length; i += 500) {
                MessageParsedText portion = new MessageParsedText(text.textInfo.subtexts.length);
                portion.textInfo = text.textInfo;
                portion.paragraphs = Arrays.copyOfRange(text.paragraphs, i, Math.min(text.paragraphs.length, i + 500));
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
