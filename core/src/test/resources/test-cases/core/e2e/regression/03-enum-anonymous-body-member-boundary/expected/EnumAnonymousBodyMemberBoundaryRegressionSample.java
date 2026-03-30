package io.github.lemon_ant.jharmonizer.core.e2e;

public enum EnumAnonymousBodyMemberBoundaryRegressionSample {
    BETA {
        @Override
        String describe(int value) {
            if (value > 10) {
                return "big";
            }

            if (value > 0) {
                return "small";
            }

            return "zero";
        }
    },

    ALPHA {
        @Override
        String describe(int value) {
            return Integer.toString(value);
        }
    };

    private final String marker = scenarioName();

    public static void main(String[] args) {
        if (!"small".equals(BETA.describe(1))) {
            throw new IllegalStateException("Unexpected BETA description");
        }
        if (!"0".equals(ALPHA.describe(0))) {
            throw new IllegalStateException("Unexpected ALPHA description");
        }
    }

    abstract String describe(int value);

    private static String scenarioName() {
        return EnumAnonymousBodyMemberBoundaryRegressionSample.class.getSimpleName();
    }
}
