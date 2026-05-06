// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e.optoutmatrix;

// This floating file-scope directive is intentionally separated from both package and imports.
// It still stays before the first declared type, so current behavior treats it as file-scope.

// @jharmonizer:sort-off

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class ZuluFloatingSortOffHelper {
    static String label() {
        return "zulu";
    }
}

public class FileSortOffFloatingBetweenPackageAndImports {
    int zebra;

    static class ZuluNested {
        String describe() {
            return "zulu";
        }
    }

    int ant;
    // @jharmonizer:fully-off
    static class InnerFullyOff{int zebra; static class LaterInner{static String describe(){return "later-inner";}} int ant; String describe(){return LaterInner.describe()+zebra+ant;}}

    static class AlphaNested {
        String describe() {
            return "alpha";
        }
    }

    public static void main(String[] args) {
        System.out.println(new FileSortOffFloatingBetweenPackageAndImports().describe());
    }

    String describe() {
        List<String> labels = new ArrayList<>();
        labels.add(new ZuluNested().describe().toUpperCase(Locale.ROOT));
        labels.add(new AlphaNested().describe());
        labels.add(new InnerFullyOff().describe());
        return String.join("-", labels)
                + zebra
                + ant
                + ZuluFloatingSortOffHelper.label()
                + AlphaFloatingSortOffHelper.label();
    }
}

class AlphaFloatingSortOffHelper {
    static String label() {
        return "alpha";
    }
}
