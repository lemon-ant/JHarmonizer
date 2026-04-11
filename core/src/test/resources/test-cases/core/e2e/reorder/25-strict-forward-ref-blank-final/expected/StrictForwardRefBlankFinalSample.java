package io.github.lemon_ant.jharmonizer.core.e2e;

public class StrictForwardRefBlankFinalSample {
    static int b = 7;
    static final int value;
    static int z = 50;

    static {
        value = 10;
    }

    public static void main(String[] args) {
        if (value != 10 || z != 50 || b != 7) {
            throw new IllegalStateException("Unexpected field values: value=" + value + ", z=" + z + ", b=" + b);
        }
    }
}
