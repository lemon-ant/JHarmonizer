package io.github.lemon_ant.jharmonizer.sorting;

import java.io.Serial;
import lombok.experimental.StandardException;

/**
 * Thrown when the input to {@link SimplifiedDependencyAwareSorter} is invalid — e.g. duplicate items,
 * an item in multiple groups, or a cycle in the dependency graph.
 */
@StandardException
public class SortingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
}
