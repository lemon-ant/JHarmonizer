package io.github.lemon_ant.jharmonizer.core.utilities;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.experimental.UtilityClass;

/**
 * Tracks whether the JVM has started its shutdown sequence.
 *
 * <p>Registers a JVM shutdown hook on first class load that sets an internal flag when the JVM
 * begins shutting down. This allows the processing pipeline to detect an in-progress shutdown
 * (e.g., triggered by Ctrl+C) and stop cleanly instead of flooding logs with spurious warnings
 * from Spoon's thread pool shutting down.
 */
@UtilityClass
public class JvmShutdownSignal {

    private static final AtomicBoolean SHUTTING_DOWN = new AtomicBoolean(false);

    static {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> SHUTTING_DOWN.set(true), "jHarmonizer-shutdown-hook"));
        } catch (IllegalStateException e) {
            SHUTTING_DOWN.set(true);
        } catch (SecurityException e) {
            // Shutdown hook registration is not permitted in this environment.
            // Leave the flag unchanged so the class remains usable.
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
