/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.e2e;

public class StaticInstanceInitializerSample {
    private static String staticOrder = "";
    private String instanceOrder = "";

    {
        instanceOrder += "I1";
    }

    static {
        staticOrder += "S1";
    }

    public static void main(String[] args) {
        StaticInstanceInitializerSample sample = new StaticInstanceInitializerSample();
        if (!"S1".equals(staticOrder)) {
            throw new IllegalStateException("Unexpected static initializer order: " + staticOrder);
        }
        if (!"I1".equals(sample.instanceOrder)) {
            throw new IllegalStateException("Unexpected instance initializer order: " + sample.instanceOrder);
        }
    }
}
