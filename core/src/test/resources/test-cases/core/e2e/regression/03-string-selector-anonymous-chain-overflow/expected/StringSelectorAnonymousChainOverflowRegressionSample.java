/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.e2e;

/**
 * Regression fixture for Spoon partial-evaluation recursion.
 *
 * <p>Failure signature observed without guard in
 * io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils
 *#findPartiallyEvaluatedExpression:
 * java.lang.StackOverflowError from spoon.support.reflect.declaration.CtPackageImpl.getQualifiedName(...)
 * while partially evaluating nested CtNewClass initializers.
 *
 * <p>Fix location: short-circuit on expression.getElements(new TypeFilter<>(CtNewClass.class)) before
 * expression.partiallyEvaluate().
 */
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
