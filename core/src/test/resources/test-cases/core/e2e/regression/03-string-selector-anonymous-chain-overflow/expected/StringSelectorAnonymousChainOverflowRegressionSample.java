package io.github.lemon_ant.jharmonizer.core.e2e;

public class StringSelectorAnonymousChainOverflowRegressionSample {
    private static final Selector selector = Selector.empty().or("", "hit");

    private interface Selector {
        static Selector empty() {
            return new Selector() {
                @Override
                public Selector or(String... values) {
                    for (String value : values) {
                        if (value != null && !value.isEmpty()) {
                            return new Selector() {
                                @Override
                                public Selector or(String... ignored) {
                                    return this;
                                }

                                @Override
                                public String value() {
                                    return value;
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

        Selector or(String... values);

        String value();
    }
}
