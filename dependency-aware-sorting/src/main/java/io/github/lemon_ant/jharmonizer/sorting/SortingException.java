package io.github.lemon_ant.jharmonizer.sorting;

import lombok.experimental.StandardException;

/**
 * Thrown when the input to {@link SimplifiedDependencyAwareSorter} is invalid — e.g. duplicate items,
 * an item in multiple groups, or a cycle in the dependency graph.
 */
@StandardException
public class SortingException extends RuntimeException {

    private static final long serialVersionUID = 1L;
}
