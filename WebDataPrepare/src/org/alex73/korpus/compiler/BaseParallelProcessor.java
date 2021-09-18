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

public abstract class BaseParallelProcessor {
    protected final ThreadPoolExecutor executor;
    private boolean queueFilled;
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
                    o.append("==================\n");
                    synchronized (instances) {
                        for (BaseParallelProcessor p : instances) {
                            o.append(p.getClass().getSimpleName() + ":");
                            o.append(" threads " + p.executor.getActiveCount() + "/" + p.executor.getMaximumPoolSize());
                            LinkedBlockingQueue<Runnable> q = (LinkedBlockingQueue<Runnable>) p.executor.getQueue();
                            int qrem = q.remainingCapacity();
                            int qsize = q.size();
                            o.append(" queue " + qsize + "/" + (qrem + qsize));
                            if (p.executor.isShutdown()) {
                                o.append(" - shutdown");
                            }
                            o.append("\n");
                        }
                    }
                    o.append("==================\n");
                    System.out.print(o);
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
        executor = new ThreadPoolExecutor(1, threadsNumber, 1, TimeUnit.MINUTES,
                new LinkedBlockingQueue<Runnable>(queueLength), new NamedThreadFactory(getClass().getSimpleName()),
                new WaitPolicy(1, TimeUnit.HOURS));
    }

    public void run(ExRunnable runnable) {
        executor.execute(() -> {
            try {
                runnable.run();
            } catch (Throwable ex) {
                ex.printStackTrace();
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
