package io.github.lemon_ant.jharmonizer.core.e2e;

public class BlankFinalOptimizedProviderPendingSample {
    private static final int STATIC_BLANK_FINAL;

    static {
        STATIC_BLANK_FINAL = 10;
    }

    private static final int B_STATIC_READ = STATIC_BLANK_FINAL + 1;

    static {
        if (false) {
            STATIC_BLANK_FINAL = -1;
        }
    }

    private final int INSTANCE_BLANK_FINAL;

    {
        INSTANCE_BLANK_FINAL = 20;
    }

    private final int A_INSTANCE_READ = INSTANCE_BLANK_FINAL + 1;

    {
        if (false) {
            INSTANCE_BLANK_FINAL = -1;
        }
    }

    public static void main(String[] args) {
        BlankFinalOptimizedProviderPendingSample sample = new BlankFinalOptimizedProviderPendingSample();
        if (B_STATIC_READ != 11 || sample.A_INSTANCE_READ != 21) {
            throw new IllegalStateException("Unexpected values: B_STATIC_READ=" + B_STATIC_READ
                    + ", A_INSTANCE_READ=" + sample.A_INSTANCE_READ);
        }
    }
}
