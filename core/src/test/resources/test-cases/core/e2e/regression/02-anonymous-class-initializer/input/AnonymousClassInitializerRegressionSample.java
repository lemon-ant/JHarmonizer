package io.github.lemon_ant.jharmonizer.core.e2e;

public class AnonymousClassInitializerRegressionSample {
    private static final Selector selector = Selector.of().or("", "selected");
    private static final String expectedValue = "selected";

    public static void main(String[] args) {
        String selectedValue = selector.value();
        if (!expectedValue.equals(selectedValue)) {
            throw new IllegalStateException("Unexpected selected value: " + selectedValue);
        }
    }

    private interface Selector {
        Selector or(String... candidates);

        String value();

        static Selector of() {
            return new Selector() {
                @Override
                public Selector or(String... candidates) {
                    for (String candidate : candidates) {
                        if (candidate != null && !candidate.isEmpty()) {
                            return new Selector() {
                                @Override
                                public Selector or(String... ignoredCandidates) {
                                    return this;
                                }

                                @Override
                                public String value() {
                                    return candidate;
                                }
                            };
                        }
                    }

                    return this;
                }

                @Override
                public String value() {
                    return "";
                }
            };
        }
    }
}
