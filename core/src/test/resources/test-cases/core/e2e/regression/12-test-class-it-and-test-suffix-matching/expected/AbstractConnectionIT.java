// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Abstract base class for integration tests.
 *
 * <p>Regression test — IT suffix must select the Test Classes ordering rule.
 * The key observable: {@code @BeforeAll initSharedResources} must sort <em>before</em>
 * {@code @AfterAll doOneTimeCleanup} (alphabetically 'd' precedes 'i', so the Default Rule
 * would reverse them; only the Test Classes rule puts Setup before Teardown).</p>
 */
public abstract class AbstractConnectionIT {
    protected static final String PARTITION_KEY = "partitionKey";
    protected static final String PARTITION_KEY_PREFIX = "partition.value.";
    protected static final String TABLE_NAME = "tableName";
    private static final Object sharedResource = new Object();

    private static Object client;

    // JUnit Setup Methods
    @BeforeAll
    public static void initSharedResources() {
        // one-time setup — must come before @AfterAll under Test Classes rule
    }

    // JUnit Teardown Methods
    @AfterAll
    public static void doOneTimeCleanup() {
        // one-time teardown — 'd' < 'i' so Default Rule puts this first (wrong for test classes)
    }

    // JUnit Methods
    protected Map<String, Object> buildRequestItems(int count, String table, boolean withPartitionKey) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> keys = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, String> key = new HashMap<>();
            key.put(PARTITION_KEY, PARTITION_KEY_PREFIX + i);
            keys.add(key);
        }
        result.put(table, keys);
        return result;
    }

    protected Object getClient() {
        return client;
    }
}
