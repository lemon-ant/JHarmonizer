package io.github.lemon_ant.jharmonizer.sorting;

import java.io.Serial;
import lombok.NonNull;

/**
 * Thrown when the input to {@link DependencyAwareSorter} is invalid — e.g. duplicate items,
 * an item in multiple groups, an intra-group dependency that conflicts with comparator
 * ordering, or a cycle in the dependency graph.
 */
public class SortingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a sorting exception with the given message.
     *
     * @param message the detail message
     */
    public SortingException(@NonNull String message) {
        super(message);
    }
}
