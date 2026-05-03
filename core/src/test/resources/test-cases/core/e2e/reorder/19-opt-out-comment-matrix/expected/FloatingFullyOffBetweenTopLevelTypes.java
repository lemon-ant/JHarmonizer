/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.e2e.optoutmatrix;

import java.util.ArrayList;
import java.util.List;

class AlphaTopLevel {
    int ant;

    static String describe() {
        return "alpha";
    }

    int zebra;
}

// This floating directive is intentionally left orphaned between top-level types.
// Current behavior is expected to attach it to the next top-level type only.

/* @jharmonizer:fully-off */

class BetaTopLevel{int zebra; static class ZuluNested{static String describe(){return "zulu-nested";}} int ant; static class AlphaNested{static String describe(){return "alpha-nested";}} String describe(){return ZuluNested.describe()+AlphaNested.describe()+zebra+ant;}}

public class FloatingFullyOffBetweenTopLevelTypes {
    String describe() {
        List<String> labels = new ArrayList<>();
        labels.add(new ZuluTopLevel().describe());
        labels.add(new BetaTopLevel().describe());
        labels.add(AlphaTopLevel.describe());
        return String.join("-", labels);
    }

    public static void main(String[] args) {
        System.out.println(new FloatingFullyOffBetweenTopLevelTypes().describe());
    }
}

class ZuluTopLevel {
    int ant;

    static String describe() {
        return "zulu";
    }

    int zebra;
}
