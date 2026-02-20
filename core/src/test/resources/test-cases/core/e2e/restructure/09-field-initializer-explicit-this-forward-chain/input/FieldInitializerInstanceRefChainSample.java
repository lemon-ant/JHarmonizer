package e2e;

public class FieldInitializerInstanceRefChainSample {
    int h = this.e + 1;
    int e = this.b + 3;
    int b = this.h + 9;
    int c = e + 5;
    int f = this.g + 9;
    int d = this.g + 11;
    int g = 15;
    int i = this.a + 17;
    int a  = 0; // TODO Exception

    public static void main(String[] args) {
        FieldInitializerInstanceRefChainSample sample = new FieldInitializerInstanceRefChainSample();
        if (sample.a != 0
                || sample.b != 10
                || sample.c != 8
                || sample.d != 11
                || sample.e != 3
                || sample.f != 9
                || sample.g != 15
                || sample.h != 1
                || sample.i != 17) {
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
