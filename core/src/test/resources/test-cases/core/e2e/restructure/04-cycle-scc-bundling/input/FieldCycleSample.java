package e2e;

public class FieldCycleSample {
    int b = this.c + 1;
    int a = this.b + 1;
    int c = this.a + 1;

    public static void main(String[] args) {
        FieldCycleSample sample = new FieldCycleSample();
        if (sample.a != 2 || sample.b != 1 || sample.c != 3) {
            throw new IllegalStateException(
                    "Unexpected cycle field values: a=" + sample.a + ", b=" + sample.b + ", c=" + sample.c);
        }
    }
}
