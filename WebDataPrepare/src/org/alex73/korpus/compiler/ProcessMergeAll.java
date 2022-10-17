package org.alex73.korpus.compiler;

import org.alex73.korpus.compiler.BaseParallelProcessor.ExRunnable;

public class ProcessMergeAll extends BaseParallelProcessor<ExRunnable> {
    public ProcessMergeAll() {
        super(8, 8);
    }

    @Override
    public void accept(ExRunnable t) {
        run(t);
    }
}
