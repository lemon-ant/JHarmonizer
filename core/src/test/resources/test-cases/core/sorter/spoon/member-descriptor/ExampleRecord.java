package example;

public record ExampleRecord(int alpha, int beta) {
  public int sum() { return alpha + beta; }
}
