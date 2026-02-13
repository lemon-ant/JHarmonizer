package e2e;

public class FieldCycleSample {
    int b = this.c + 1;
    int a = this.b + 1;
    int c = this.a + 1;
}
