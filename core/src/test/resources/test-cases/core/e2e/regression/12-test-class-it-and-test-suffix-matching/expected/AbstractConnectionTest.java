// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Abstract base class for unit tests.
 *
 * <p>Regression test — Test suffix must select the Test Classes ordering rule.
 * The key observable: {@code @BeforeEach initTest} must sort <em>before</em>
 * {@code @AfterEach doCleanup} and the {@code @Test} method, even though
 * alphabetically 'd' precedes 'i'.</p>
 */
public abstract class AbstractConnectionTest {
    public static final String DEFAULT_TABLE = "defaultTable";
    public static final String REGION = "us-west-2";
    private static final List<String> ERROR_ATTRIBUTES = Arrays.asList("errorCode", "errorMessage", "requestId");

    protected Object client;

    @BeforeEach
    protected void initTest() {
        // per-test setup — must come before @AfterEach under Test Classes rule
    }

    @Test
    protected void verifyErrorAttributes() {
        // verify test method
    }

    @AfterEach
    protected void doCleanup() {
        // per-test teardown — 'd' < 'i' so Default Rule puts this first (wrong for test classes)
    }

    protected static String buildErrorMessage(List<String> presentAttributes) {
        return String.join(";", presentAttributes);
    }
}
