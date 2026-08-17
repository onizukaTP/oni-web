package com.onizuka.framework.server;

import com.onizuka.framework.util.OniLogger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkerThreadPool {

    private static final OniLogger log = OniLogger.get(WorkerThreadPool.class);
    private final ExecutorService executor;

    public WorkerThreadPool(int threads) {
        AtomicInteger count = new AtomicInteger(1);
        this.executor = Executors.newFixedThreadPool(threads, r -> {
            Thread thread = new Thread(r, "oni-worker-" + count.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
        log.info("Initialized worker thread pool with " + threads + " threads");
    }

    public void execute(Runnable task) {
        executor.execute(task);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
