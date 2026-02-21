package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.fixtures;

public class FieldInitializerExplicitThisForwardReferenceNullDefaultValueFixture {

    private final int alpha = this.bravo == null ? 1 : 0;

    private final Object bravo = null;
}
