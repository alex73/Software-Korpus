package org.alex73.korpus.compiler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class QueueProcessor<T> {
    static final int PARALLELS = 12;
    static final Object FINISH = new Object();
    ExecutorService threads;
    BlockingQueue<Object> queue;

    public QueueProcessor() {
        threads = Executors.newFixedThreadPool(PARALLELS);
        queue = new ArrayBlockingQueue<>(PARALLELS);
        for (int i = 0; i < PARALLELS; i++) {
            threads.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        while (true) {
                            Object obj = queue.take();
                            if (obj == FINISH) {
                                return;
                            }
                            process((T) obj);
                        }
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
    }

    public void put(T obj) throws InterruptedException {
        queue.put(obj);
    }

    public void fin() throws Exception {
        for (int i = 0; i < PARALLELS; i++) {
            queue.put(FINISH);
        }
        threads.shutdown();
        threads.awaitTermination(1, TimeUnit.DAYS);
    }

    public abstract void process(T obj) throws Exception;
}
