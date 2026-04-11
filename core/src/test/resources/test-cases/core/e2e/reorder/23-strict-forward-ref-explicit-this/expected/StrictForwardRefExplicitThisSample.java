package io.github.lemon_ant.jharmonizer.core.e2e;

public class StrictForwardRefExplicitThisSample {
    int a = 10;
    int z = this.a + 1;
    int b = 5;
    int d = 7;
    int m = 50;

    public static void main(String[] args) {
        StrictForwardRefExplicitThisSample sample = new StrictForwardRefExplicitThisSample();
        if (sample.a != 10 || sample.z != 11 || sample.m != 50 || sample.b != 5 || sample.d != 7) {
            throw new IllegalStateException("Unexpected field values:"
                    + " a=" + sample.a
                    + ", z=" + sample.z
                    + ", m=" + sample.m
                    + ", b=" + sample.b
                    + ", d=" + sample.d);
        }
    }
}
