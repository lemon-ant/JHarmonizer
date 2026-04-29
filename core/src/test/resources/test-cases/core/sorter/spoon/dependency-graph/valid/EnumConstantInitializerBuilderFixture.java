package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

public enum EnumConstantInitializerBuilderFixture {

    BRAVO(null),
    ALPHA(BRAVO);

    private final EnumConstantInitializerBuilderFixture predecessor;

    EnumConstantInitializerBuilderFixture(EnumConstantInitializerBuilderFixture predecessor) {
        this.predecessor = predecessor;
    }
}
