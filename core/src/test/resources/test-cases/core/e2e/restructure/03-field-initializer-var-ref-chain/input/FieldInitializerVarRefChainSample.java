package e2e;

public class FieldInitializerVarRefChainSample {
    int g = 25;
    int i = g + 27;
    int f = 21 + g;
    int e = 15;
    int h = 1;
    int d = h + 3;
    int b = d + 9;
    int c = 5;
    int a = 12;

    public static void main(String[] args) {
        FieldInitializerVarRefChainSample sample = new FieldInitializerVarRefChainSample();
        if (sample.a != 12
                || sample.b != 13
                || sample.c != 5
                || sample.d != 4
                || sample.e != 15
                || sample.f != 46
                || sample.g != 25
                || sample.h != 1
                || sample.i != 52) {
            throw new IllegalStateException(
                    "Unexpected field values after initialization:"
                            + " a="
                            + sample.a
                            + ", b="
                            + sample.b
                            + ", c="
                            + sample.c
                            + ", d="
                            + sample.d
                            + ", e="
                            + sample.e
                            + ", f="
                            + sample.f
                            + ", g="
                            + sample.g
                            + ", h="
                            + sample.h
                            + ", i="
                            + sample.i);
        }
    }
}
