// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.concurrent.ExecutorService;

public abstract class TestListenerMultiLine {
    private final Object config;

    private volatile Object conn; // keep volatile
    private volatile ExecutorService executor =
            java.util.concurrent.Executors.newSingleThreadExecutor(); // keep volatile
}
