package e2e;

public class FieldInitializerInstanceRefChainSample {
    int c = this.b + 1;
    int b = this.a + 3;
    int a = this.c + 9;

    public static void main(String[] args) {
        FieldInitializerInstanceRefChainSample sample = new FieldInitializerInstanceRefChainSample();
        if (sample.a != 10 || sample.b != 3 || sample.c != 1) {
            throw new IllegalStateException(
                "Unexpected field chain values: a=" + sample.a + ", b=" + sample.b + ", c=" + sample.c);
        }
    }
}
