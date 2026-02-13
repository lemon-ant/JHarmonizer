package e2e;

public class FieldInitializerChainSample {
    int a = 1;
    int b = this.a + 1;
    int c = this.b + 1;
}
