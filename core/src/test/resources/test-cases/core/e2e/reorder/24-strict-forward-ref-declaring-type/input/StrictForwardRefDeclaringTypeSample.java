package io.github.lemon_ant.jharmonizer.core.e2e;

public class StrictForwardRefDeclaringTypeSample {
    static int a = 10;
    static int z = StrictForwardRefDeclaringTypeSample.a + 1;
    static int m = 50;
    static int b = 5;
    static int d = 7;

    public static void main(String[] args) {
        if (a != 10 || z != 11 || m != 50 || b != 5 || d != 7) {
            throw new IllegalStateException("Unexpected field values:"
                    + " a=" + a + ", z=" + z + ", m=" + m + ", b=" + b + ", d=" + d);
        }
    }
}
