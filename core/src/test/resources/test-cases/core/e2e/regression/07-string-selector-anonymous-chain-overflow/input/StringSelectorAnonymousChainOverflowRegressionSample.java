package io.github.lemon_ant.jharmonizer.core.e2e;

public class StringSelectorAnonymousChainOverflowRegressionSample {
    private static final String expectedValue = "selected";
    private static final StringSelector selector = StringSelector.of((String) null, "", "selected");

    public static void main(String[] args) {
        String selectedValue = selector.toString();
        if (!expectedValue.equals(selectedValue)) {
            throw new IllegalStateException("Unexpected selected value: " + selectedValue);
        }
    }

    private interface StringSelector {
        StringSelector EMPTY_STRING_SELECTOR = new StringSelector() {
            @Override
            public String toString() {
                return "";
            }

            @Override
            public StringSelector or(String... strings) {
                for (String string : strings) {
                    if (string != null && !string.isEmpty()) {
                        return new StringSelector() {
                            @Override
                            public StringSelector or(String... ignoredStrings) {
                                return this;
                            }

                            @Override
                            public String toString() {
                                return string;
                            }
                        };
                    }
                }

                return EMPTY_STRING_SELECTOR;
            }
        };

        static StringSelector of(String... strings) {
            return EMPTY_STRING_SELECTOR.or(strings);
        }

        StringSelector or(String... strings);
    }
}
