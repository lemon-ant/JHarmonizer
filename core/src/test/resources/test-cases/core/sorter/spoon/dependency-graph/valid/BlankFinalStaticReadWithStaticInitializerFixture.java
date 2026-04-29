package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

public class BlankFinalStaticReadWithStaticInitializerFixture {

    private static final int STATIC_BLANK_FINAL;

    static {
        STATIC_BLANK_FINAL = 10;
    }

    private static final int B_STATIC_READ = STATIC_BLANK_FINAL + 1;
}
