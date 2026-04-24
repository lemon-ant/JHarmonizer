// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.concurrent.ExecutorService;

public abstract class TestListenerMultiLine {

    private volatile ExecutorService executor =
            java.util.concurrent.Executors.newSingleThreadExecutor(); // keep volatile
    private volatile Object conn;              // keep volatile
    private final Object config;

}
