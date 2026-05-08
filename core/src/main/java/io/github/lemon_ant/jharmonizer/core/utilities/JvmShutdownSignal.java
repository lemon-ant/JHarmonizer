// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.utilities;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Tracks whether the JVM has started its shutdown sequence.
 *
 * <p>Registers a JVM shutdown hook on first class load that sets an internal flag when the JVM
 * begins shutting down. This allows the processing pipeline to detect an in-progress shutdown
 * (e.g., triggered by Ctrl+C) and stop cleanly instead of flooding logs with spurious warnings
 * from Spoon's thread pool shutting down.
 */
@Slf4j
@SuppressWarnings("PMD.DoNotUseThreads")
@UtilityClass
public class JvmShutdownSignal {
    private static final AtomicBoolean SHUTTING_DOWN = new AtomicBoolean(false);

    static {
        try {
            Runtime.getRuntime()
                    .addShutdownHook(new Thread(() -> SHUTTING_DOWN.set(true), "jHarmonizer-shutdown-hook"));
        } catch (IllegalStateException e) {
            SHUTTING_DOWN.set(true);
        } catch (SecurityException e) {
            // Shutdown hook registration is not permitted in this environment.
            // Leave the flag unchanged so the class remains usable.
            log.debug("JVM shutdown hook registration denied; running without shutdown detection.", e);
        }
    }

    /**
     * Returns {@code true} if the JVM has started its shutdown sequence (e.g., Ctrl+C was pressed).
     *
     * @return {@code true} if the JVM is shutting down
     */
    public static boolean isShuttingDown() {
        return SHUTTING_DOWN.get();
    }
}
