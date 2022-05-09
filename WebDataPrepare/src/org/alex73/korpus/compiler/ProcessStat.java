package org.alex73.korpus.compiler;

import java.nio.file.Path;
import java.util.List;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.text.structure.corpus.Paragraph;

public class ProcessStat extends BaseParallelProcessor {
    private final StatProcessing textStat;

    public ProcessStat(boolean processStat) throws Exception {
        super(8, 8);
        if (processStat) {
            textStat = new StatProcessing();
        } else {
            textStat = null;
        }
    }

    public void process(TextInfo textInfo, List<Paragraph> content) {
        if (textStat == null) {
            return;
        }
        run(() -> {
            textStat.add(textInfo, content);
        });
    }

    public void finish(Path output) throws Exception {
        super.finish(10);
        if (textStat != null) {
            textStat.write(output);
        }
    }
}
