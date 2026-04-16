package io.github.lemon_ant.jharmonizer.core.e2e;

class NestedMultiNodeCycleRegressionSample {
    static final String A_VALUE = new String("A");
    static final String B_VALUE = new String("B");
    static final String Z_RESULT = Mode.ENTRY.getValue();

    public static void main(String[] args) {
        if (!"A+B".equals(Z_RESULT)) {
            throw new IllegalStateException("Expected 'A+B' but was: " + Z_RESULT);
        }
    }

    static class Constants {
        static final String COMBINED = new String(
                NestedMultiNodeCycleRegressionSample.A_VALUE + "+" + NestedMultiNodeCycleRegressionSample.B_VALUE);
    }

    enum Mode {
        ENTRY(Constants.COMBINED);

        private final String value;

        String getValue() {
            return value;
        }

        Mode(String value) {
            this.value = value;
        }
    }
}
