package io.github.lemon_ant.jharmonizer.core.e2e;

public class StrictBlankFinalSample {
    static final int VALUE;
    static int alpha = 1;

    public static void main(String[] args) {
        if (alpha != 1) {
            throw new IllegalStateException("Unexpected field value: alpha=" + alpha);
        }
    }

    static {
        VALUE = 42;
    }

    static int zReader = VALUE + 1;
}
