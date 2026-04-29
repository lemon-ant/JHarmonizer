package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

public class FieldInitializerExplicitThisForwardReferenceWithStaticReferrerFixture {

    private static final int staticProbe = 123;
    private final int alpha = this.bravo + 1;
    private final int bravo = 10;
}
