package com.codewise.httpclientbench.reproducer;

import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public abstract class Reproducer implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(Reproducer.class);
    protected static final int START_PORT = 9080;
    protected static final int END_PORT = 9099;
    protected static final int THREADS = 10;
    protected static final int REQUEST_INTERVAL = 100;
    protected final ServerRange serverRange;
    protected final ExecutorService executorService;
    protected final AtomicLong errorCount = new AtomicLong(0);
    protected final AtomicLong successCount = new AtomicLong(0);
    protected volatile long lastDisplayedLogs = 0;

    public Reproducer() {
        this.serverRange = new ServerRange(START_PORT, END_PORT);
        this.executorService = Executors.newFixedThreadPool(THREADS);
    }

    protected void start() throws InterruptedException {
        log.info("Starting environment");
        while (true) {
            Thread.sleep(REQUEST_INTERVAL);
            printStats();
            for (int i = 0; i < THREADS; i++) {
                executorService.submit(this::callAllServers);
            }
        }
    }

    private void printStats() {
        if (System.currentTimeMillis() - lastDisplayedLogs > 1000) {
            lastDisplayedLogs = System.currentTimeMillis();
            log.info("Success {}, Errors {}", successCount, errorCount);
        }
    }

    protected void callAllServers() {
        Observable.range(START_PORT, END_PORT - START_PORT + 1)
                .flatMap(this::callSingleServer)
                .subscribe();
    }

    protected abstract Observable<String> callSingleServer(int port);

    @Override
    public void close() throws IOException {
        serverRange.close();
        executorService.shutdown();
    }
}
