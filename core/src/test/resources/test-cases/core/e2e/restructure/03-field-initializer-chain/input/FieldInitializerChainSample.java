package e2e;

public class FieldInitializerChainSample {
    int c = this.b + 1;
    int b = this.a + 1;
    int a = 1;
}
