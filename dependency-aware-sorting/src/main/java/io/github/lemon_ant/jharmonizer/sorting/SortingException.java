package io.github.lemon_ant.jharmonizer.sorting;

/**
 * Thrown when the input to {@link DependencyAwareSorter} is invalid — e.g. duplicate items,
 * an item in multiple groups, an intra-group dependency that conflicts with comparator
 * ordering, or a cycle in the dependency graph.
 */
public class SortingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SortingException(String message) {
        super(message);
    }
}
