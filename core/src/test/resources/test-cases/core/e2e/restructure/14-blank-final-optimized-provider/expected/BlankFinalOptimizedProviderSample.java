package io.github.lemon_ant.jharmonizer.core.e2e;

public class BlankFinalOptimizedProviderSample {

    private static final int STATIC_BLANK_FINAL;

    static {
        STATIC_BLANK_FINAL = 10;
    }

    private static final int B_STATIC_READ = STATIC_BLANK_FINAL + 1;

    public static void main(String[] args) {
        if (B_STATIC_READ != 11) {
            throw new IllegalStateException("Unexpected values: B_STATIC_READ=" + B_STATIC_READ);
        }
    }
}
