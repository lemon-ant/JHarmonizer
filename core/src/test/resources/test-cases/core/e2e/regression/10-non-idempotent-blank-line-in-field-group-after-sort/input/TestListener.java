// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class TestListener {

    private static final int DEFAULT_WAIT_SECONDS = 5;
    private static final AtomicInteger counter = new AtomicInteger(0);
    private volatile ExecutorService executor; // keep volatile
    private volatile Object connection;        // keep volatile
    private final int numWorkers;
    private final int port;
    private final CustomConfig config;
    private final AtomicInteger waitSeconds = new AtomicInteger(DEFAULT_WAIT_SECONDS);

}
