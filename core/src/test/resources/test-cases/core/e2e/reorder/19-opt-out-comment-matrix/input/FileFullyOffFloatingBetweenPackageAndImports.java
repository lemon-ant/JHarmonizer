/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.e2e.optoutmatrix;

// This floating file-scope directive is intentionally separated from both package and imports.
// It still stays before the first declared type, so current behavior treats it as file-scope.

/* @jharmonizer:fully-off */

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class ZuluFloatingFullyOffHelper{static String label(){return "zulu";}}
public class FileFullyOffFloatingBetweenPackageAndImports{int zebra;
    static class ZuluNested{String describe(){return "zulu";}}
    int ant;
    // @jharmonizer:sort-off
    static class InnerSortOff{int zebra; static class ZuluInner{static String describe(){return "zulu-inner";}} int ant; static class AlphaInner{static String describe(){return "alpha-inner";}} String describe(){return ZuluInner.describe()+AlphaInner.describe()+zebra+ant;}}
    // @jharmonizer:fully-off
    static class InnerFullyOff{int zebra; static class LaterInner{static String describe(){return "later-inner";}} int ant; String describe(){return LaterInner.describe()+zebra+ant;}}
    public static void main(String[] args){System.out.println(new FileFullyOffFloatingBetweenPackageAndImports().describe());}
    String describe(){List<String> labels=new ArrayList<>(); labels.add(new ZuluNested().describe().toUpperCase(Locale.ROOT)); labels.add(new InnerSortOff().describe()); labels.add(new InnerFullyOff().describe()); return String.join("-",labels)+zebra+ant+ZuluFloatingFullyOffHelper.label()+AlphaFloatingFullyOffHelper.label();}
}
class AlphaFloatingFullyOffHelper{static String label(){return "alpha";}}
