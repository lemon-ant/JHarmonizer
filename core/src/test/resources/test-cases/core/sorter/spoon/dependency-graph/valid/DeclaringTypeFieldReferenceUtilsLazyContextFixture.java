package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

public class DeclaringTypeFieldReferenceUtilsLazyContextFixture {
    int value;
    int source = 10;

    Runnable lambdaWriter = () -> {
        value = source;
    };
}
