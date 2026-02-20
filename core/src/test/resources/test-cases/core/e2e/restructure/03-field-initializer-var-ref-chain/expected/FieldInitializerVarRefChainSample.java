package e2e;

public class FieldInitializerVarRefChainSample {
    int c = 1;
    int b = c + 3;
    int a = b + 9;

    public static void main(String[] args) {
        FieldInitializerVarRefChainSample sample = new FieldInitializerVarRefChainSample();
        if (sample.a != 13 || sample.b != 4 || sample.c != 1) {
            throw new IllegalStateException(
                    "Unexpected field chain values: a=" + sample.a + ", b=" + sample.b + ", c=" + sample.c);
        }
    }
}
