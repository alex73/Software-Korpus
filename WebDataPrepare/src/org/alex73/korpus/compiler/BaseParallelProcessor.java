package org.alex73.korpus.compiler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseParallelProcessor {
    protected final ThreadPoolExecutor executor;
    private boolean queueFilled;
    public int defaultThreadPriority = Thread.NORM_PRIORITY;

    public BaseParallelProcessor(int threadsNumber, int queueLength) {
        executor = new ThreadPoolExecutor(1, threadsNumber, 1, TimeUnit.MINUTES,
                new LinkedBlockingQueue<Runnable>(queueLength), new NamedThreadFactory(getClass().getSimpleName()),
                new WaitPolicy(1, TimeUnit.HOURS));
    }

    public void run(ExRunnable runnable) {
        executor.execute(() -> {
            try {
                runnable.run();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public void finish(int minutes) throws Exception {
        executor.shutdown();
        executor.awaitTermination(minutes, TimeUnit.MINUTES);
        int thLoad = executor.getLargestPoolSize();
        int thMax = executor.getMaximumPoolSize();
        System.out.println(getClass().getSimpleName() + " stat: " + thLoad + "/" + thMax + " threads, queue "
                + (queueFilled ? "was blocked" : "was not blocked"));
    }

    protected class NamedThreadFactory implements ThreadFactory {
        private final String name;
        private final AtomicInteger n = new AtomicInteger();

        public NamedThreadFactory(String name) {
            this.name = name;
        }

        public Thread newThread(Runnable r) {
            Thread th= new Thread(r, name + "-" + n.incrementAndGet());
            th.setPriority(defaultThreadPriority);
            return th;
        }
    }

    protected class WaitPolicy implements RejectedExecutionHandler {
        private final long timeout;
        private final TimeUnit unit;

        public WaitPolicy(long timeout, TimeUnit unit) {
            this.timeout = timeout;
            this.unit = unit;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                try {
                    BlockingQueue<Runnable> queue = executor.getQueue();
                    if (queue.remainingCapacity() == 0) {
                        queueFilled = true;
                    }
                    if (!queue.offer(r, timeout, unit)) {
                        throw new RejectedExecutionException("Max wait time expired to queue task");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RejectedExecutionException("Interrupted", e);
                }
            } else {
                throw new RejectedExecutionException("Executor has been shut down");
            }
        }
    }

    @FunctionalInterface
    public interface ExRunnable {
        public abstract void run() throws Exception;
    }
}
