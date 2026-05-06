// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
// @jharmonizer:fully-off
package io.github.lemon_ant.jharmonizer.core.e2e.optoutmatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class ZuluPackageHelper{static String label(){return "zulu-package";}}
public class FileFullyOffBeforePackageWithImports{int zebra;
    static class ZuluNested{String describe(){return "zulu";}}
    int ant;
    // @jharmonizer:sort-off
    static class InnerSortOff{int zebra; static class BetaNested{static String describe(){return "beta";}} int ant; static class AlphaNested{static String describe(){return "alpha";}} String describe(){return BetaNested.describe()+new AlphaNested().describe()+zebra+ant;}}
    // @jharmonizer:fully-off
    static class InnerFullyOff{int zebra; static class LaterNested{static String describe(){return "later";}} int ant; String describe(){return LaterNested.describe()+zebra+ant;}}
    public static void main(String[] args){System.out.println(new FileFullyOffBeforePackageWithImports().describe());}
    String describe(){List<String> labels=new ArrayList<>(); labels.add(new InnerSortOff().describe()); labels.add(new InnerFullyOff().describe()); labels.add(new ZuluNested().describe().toUpperCase(Locale.ROOT)); return String.join("-",labels)+zebra+ant+ZuluPackageHelper.label()+AlphaPackageHelper.label();}
}
class AlphaPackageHelper{static String label(){return "alpha-package";}}
