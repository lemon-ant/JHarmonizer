package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

public class BlankFinalLazyWriteInLambdaBuilderFixture {
    final int BLANK_FINAL;

    Runnable PROVIDER = () -> {
        // Intentionally lazy write. This fixture protects write-scan behavior in dependency graph building.
        this.BLANK_FINAL = 42;
    };

    int READ_AFTER_ASSIGNMENT = BLANK_FINAL;
}
