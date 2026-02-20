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
        if (sample.b != 13 || sample.d != 4 || sample.h != 1) {
            throw new IllegalStateException(
                    "Unexpected field chain values: b=" + sample.b + ", d=" + sample.d + ", h=" + sample.h);
        }
    }
}
