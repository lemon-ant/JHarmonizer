package e2e;

public class FieldInitializerChainSample {
    int a = this.b + 1;
    int b = this.c + 1;
    int c = this.a + 1;

    public static void main(String[] args) {
        FieldInitializerChainSample sample = new FieldInitializerChainSample();
        if (sample.a != 1 || sample.b != 1 || sample.c != 2) {
            throw new IllegalStateException(
                    "Unexpected field chain values: a=" + sample.a + ", b=" + sample.b + ", c=" + sample.c);
        }
    }
}
