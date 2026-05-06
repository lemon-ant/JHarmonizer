// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e.optoutmatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FloatingSortOffBetweenNestedTypes {
    String describe() {
        List<String> labels = new ArrayList<>();
        labels.add(new ZuluNested().describe().toUpperCase(Locale.ROOT));
        labels.add(new BetaAttached().describe());
        labels.add(new AlphaNested().describe());
        return String.join("-", labels);
    }

    static class AlphaNested {
        int ant;

        static String describe() {
            return "alpha";
        }

        int zebra;
    }

    // This floating directive is intentionally left orphaned between sibling nested types.
    // Current behavior is expected to attach it to the next nested type only.

    // @jharmonizer:sort-off

    static class BetaAttached {
        int zebra;

        static class ZuluInner {
            static String describe() {
                return "zulu-inner";
            }
        }

        int ant;

        static class AlphaInner {
            static String describe() {
                return "alpha-inner";
            }
        }

        String describe() {
            return ZuluInner.describe() + AlphaInner.describe() + zebra + ant;
        }
    }

    static class ZuluNested {
        int ant;

        static String describe() {
            return "zulu";
        }

        int zebra;
    }

    public static void main(String[] args) {
        System.out.println(new FloatingSortOffBetweenNestedTypes().describe());
    }
}
