package io.github.lemon_ant.jharmonizer.core.e2e;

public class BlankFinalNearestProviderRegressionSample {
    private static final int STATIC_BLANK_FINAL;

    static {
        if (Boolean.getBoolean("blank.final.switch")) {
            STATIC_BLANK_FINAL = 10;
        } else {
            STATIC_BLANK_FINAL = 20;
        }
    }

    private static final int B_STATIC_READ = STATIC_BLANK_FINAL + 1;
    private static int staticNoise = 0;

    static {
        staticNoise += 10;
    }

    private final int INSTANCE_BLANK_FINAL;

    {
        if (System.nanoTime() >= 0) {
            INSTANCE_BLANK_FINAL = 30;
        } else {
            INSTANCE_BLANK_FINAL = 40;
        }
    }

    private final int A_INSTANCE_READ = INSTANCE_BLANK_FINAL + 1;
    private int instanceNoise = 0;

    {
        instanceNoise += 10;
    }

    public static void main(String[] args) {
        BlankFinalNearestProviderRegressionSample sample = new BlankFinalNearestProviderRegressionSample();

        if ((B_STATIC_READ != 11 && B_STATIC_READ != 21)
                || (sample.A_INSTANCE_READ != 31 && sample.A_INSTANCE_READ != 41)) {
            throw new IllegalStateException("Unexpected values: B_STATIC_READ=" + B_STATIC_READ + ", A_INSTANCE_READ="
                    + sample.A_INSTANCE_READ);
        }

        if (staticNoise != 10 || sample.instanceNoise != 10) {
            throw new IllegalStateException(
                    "Unexpected noise values: staticNoise=" + staticNoise + ", instanceNoise=" + sample.instanceNoise);
        }
    }
}
