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
    int a  = 0;

    public static void main(String[] args) {
        FieldInitializerInstanceRefChainSample sample = new FieldInitializerInstanceRefChainSample();
        if (sample.b != 10 || sample.e != 3 || sample.h != 1) {
            throw new IllegalStateException(
                "Unexpected field chain values: b=" + sample.b + ", e=" + sample.e + ", h=" + sample.h);
        }
    }
}
