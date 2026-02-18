package e2e;

public class FieldInitializerChainSample {
    int a = this.b + 1;
    int b = this.c + 1;
    int c = this.a + 1;
}
