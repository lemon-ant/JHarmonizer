/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class InitializerBlockMutableFieldReadBuilderFixture {

    private static final Map<Integer, String> ITEM_REGISTRY = new HashMap<>();

    static {
        ITEM_REGISTRY.put(1, "alpha");
        ITEM_REGISTRY.put(2, "beta");
    }

    static final List<String> REGISTRY_SNAPSHOT = List.copyOf(ITEM_REGISTRY.values());
}
