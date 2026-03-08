package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

public class BlankFinalNearestProviderSelectionFixture {

    private static final int STATIC_BLANK_FINAL;

    static {
        STATIC_BLANK_FINAL = 1;
    }

    static {
        STATIC_BLANK_FINAL = 2;
    }

    private static final int STATIC_READ = STATIC_BLANK_FINAL;

    private final int INSTANCE_BLANK_FINAL;

    {
        INSTANCE_BLANK_FINAL = 10;
    }

    {
        INSTANCE_BLANK_FINAL = 20;
    }

    private final int INSTANCE_READ = INSTANCE_BLANK_FINAL;
}
