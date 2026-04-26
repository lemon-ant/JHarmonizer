// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.ParameterizedTest;

/**
 * Regression test — @ParameterizedTest, @RepeatedTest, @TestFactory, @TestTemplate must sort
 * alongside @Test methods alphabetically in the JUnit Test Methods group, not fall through to
 * the parent bucket.
 */
public class SampleParameterizedTest {

    @BeforeEach
    void setUp() {
        // setup
    }

    @TestFactory
    void aFactory() {
        // test factory — 'a' sorts first in JUnit Test Methods
    }

    @RepeatedTest(3)
    void bRepeated() {
        // repeated test — 'b' sorts second in JUnit Test Methods
    }

    @Test
    void cTest() {
        // regular test — 'c' sorts third in JUnit Test Methods
    }

    @TestTemplate
    void mTemplate() {
        // test template — 'm' sorts fourth in JUnit Test Methods
    }

    @ParameterizedTest(name = "c2Client={0} bundleFileList={1} contentFilter={2}")
    void zParam(Object c2Client, Object bundleFileList, Object contentFilter) {
        // parameterized test — 'z' sorts last in JUnit Test Methods
    }

    @AfterEach
    void zTearDown() {
        // teardown — intentionally listed before setUp to expose wrong ordering without fix
    }

    static void zHelper() {
        // utility helper — goes to Utility Methods section
    }
}
