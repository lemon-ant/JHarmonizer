package io.github.lemon_ant.jharmonizer.core.e2e;

public class BlankFinalOptimizedProviderPendingSample {
    private static final int STATIC_BLANK_FINAL;
    private static int staticNoise = 0;

    static {
        staticNoise++;
    }

    static {
        STATIC_BLANK_FINAL = 10;
    }

    private static final int B_STATIC_READ = STATIC_BLANK_FINAL + 1;

    private final int INSTANCE_BLANK_FINAL;
    private int instanceNoise = 0;

    {
        instanceNoise++;
    }

    {
        INSTANCE_BLANK_FINAL = 20;
    }

    private final int A_INSTANCE_READ = INSTANCE_BLANK_FINAL + 1;

    public static void main(String[] args) {
        BlankFinalOptimizedProviderPendingSample sample = new BlankFinalOptimizedProviderPendingSample();
        if (B_STATIC_READ != 11 || sample.A_INSTANCE_READ != 21) {
            throw new IllegalStateException("Unexpected values: B_STATIC_READ=" + B_STATIC_READ
                    + ", A_INSTANCE_READ=" + sample.A_INSTANCE_READ);
        }

        if (staticNoise != 1 || sample.instanceNoise != 1) {
            throw new IllegalStateException("Unexpected noise values: staticNoise=" + staticNoise
                    + ", instanceNoise=" + sample.instanceNoise);
        }
    }
}
