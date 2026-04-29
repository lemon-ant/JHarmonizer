package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

public record RecordWithExplicitMethodAndNoImplicitMembers(int alpha, int beta) {
  public int sum() { return alpha + beta; }
}
