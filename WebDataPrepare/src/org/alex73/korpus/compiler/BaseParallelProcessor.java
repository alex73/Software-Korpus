package org.alex73.korpus.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseParallelProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(BaseParallelProcessor.class);

    protected final ThreadPoolExecutor executor;
    private int queueCapacity;
    public int defaultThreadPriority = Thread.NORM_PRIORITY;
    private static List<BaseParallelProcessor> instances = new ArrayList<>();
    private static Thread statThread;

    public static void startStat() {
        statThread = new Thread() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException ex) {
                        break;
                    }
                    StringBuilder o = new StringBuilder();
                    synchronized (instances) {
                        for (BaseParallelProcessor p : instances) {
                            o.append("\t" + p.getClass().getSimpleName() + ":");
                            o.append(" threads " + p.executor.getActiveCount() + "/" + p.executor.getMaximumPoolSize());
                            LinkedBlockingQueue<Runnable> q = (LinkedBlockingQueue<Runnable>) p.executor.getQueue();
                            o.append(" queue " + q.size() + "/" + p.queueCapacity);
                            if (p.executor.isShutdown()) {
                                o.append(" - shutdown");
                            }
                            o.append("\n");
                        }
                    }
                    LOG.info("Statistics: \n" + o);
                }
            }
        };
        statThread.start();
    }

    public static void stopStat() {
        if (statThread != null) {
            statThread.interrupt();
        }
    }

    public BaseParallelProcessor(int threadsNumber, int queueLength) {
        synchronized (instances) {
            instances.add(this);
        }
        this.queueCapacity = queueLength;
        executor = new ThreadPoolExecutor(1, threadsNumber, 1, TimeUnit.MINUTES,
                new LinkedBlockingQueue<Runnable>(queueLength), new NamedThreadFactory(getClass().getSimpleName()),
                new WaitPolicy(1, TimeUnit.HOURS));
    }

    public void run(ExRunnable runnable) {
        executor.execute(() -> {
            try {
                runnable.run();
            } catch (OutOfMemoryError ex) {
                LOG.error("", ex);
                System.exit(1);
            } catch (Throwable ex) {
                LOG.error("", ex);
            }
        });
    }

    public void finish(int minutes) throws Exception {
        executor.shutdown();
        executor.awaitTermination(minutes, TimeUnit.MINUTES);
        synchronized (instances) {
            instances.remove(this);
        }
    }

    protected class NamedThreadFactory implements ThreadFactory {
        private final String name;
        private final AtomicInteger n = new AtomicInteger();

        public NamedThreadFactory(String name) {
            this.name = name;
        }

        public Thread newThread(Runnable r) {
            Thread th = new Thread(r, name + "-" + n.incrementAndGet());
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
