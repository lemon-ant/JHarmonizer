// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractTaskListener {

    private static final int DEFAULT_SHUTDOWN_SECONDS = 5;
    private static final Object log = new Object();
    private volatile ExecutorService executorService;  // volatile to guarantee most current value is visible
    private volatile Object serverHandle;              // volatile to guarantee most current value is visible
    private final int numWorkers;
    private final int port;
    private final Object config;
    private final AtomicInteger shutdownSeconds = new AtomicInteger(DEFAULT_SHUTDOWN_SECONDS);

    protected AbstractTaskListener(
            final int numWorkers,
            final int port,
            final Object config) {

        if (numWorkers <= 0) {
            throw new IllegalArgumentException("Number of workers may not be less than or equal to zero.");
        } else if (config == null) {
            throw new IllegalArgumentException("Config may not be null.");
        }

        this.numWorkers = numWorkers;
        this.port = port;
        this.config = config;
    }

    public abstract void handleRequest(Object request);

    public void start() {
        if (isRunning()) {
            return;
        }

        final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
        executorService = Executors.newFixedThreadPool(numWorkers, new ThreadFactory() {
            private final AtomicLong threadCounter = new AtomicLong(0L);

            @Override
            public Thread newThread(final Runnable r) {
                final Thread newThread = defaultThreadFactory.newThread(r);
                newThread.setName("Worker-" + threadCounter.incrementAndGet());
                return newThread;
            }
        });
    }

    public boolean isRunning() {
        return (executorService != null && !executorService.isShutdown());
    }

    public void stop() throws InterruptedException {
        if (!isRunning()) {
            return;
        }

        try {
            if (getShutdownSeconds() <= 0) {
                executorService.shutdownNow();
            } else {
                executorService.shutdown();
            }
            executorService.awaitTermination(getShutdownSeconds(), TimeUnit.SECONDS);
        } finally {
            if (executorService.isTerminated()) {
                System.out.println("Task listener terminated successfully.");
            } else {
                System.out.println("Task listener has not terminated properly.");
            }
        }
    }

    public int getShutdownSeconds() {
        return shutdownSeconds.get();
    }

    public void setShutdownSeconds(final int seconds) {
        shutdownSeconds.set(seconds);
    }

    public Object getConfig() {
        return config;
    }

    public int getPort() {
        return port;
    }

}
