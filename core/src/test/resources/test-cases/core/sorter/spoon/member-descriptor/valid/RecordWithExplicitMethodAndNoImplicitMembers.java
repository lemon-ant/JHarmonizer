package example;

public record RecordWithExplicitMethodAndNoImplicitMembers(int alpha, int beta) {
  public int sum() { return alpha + beta; }
}
