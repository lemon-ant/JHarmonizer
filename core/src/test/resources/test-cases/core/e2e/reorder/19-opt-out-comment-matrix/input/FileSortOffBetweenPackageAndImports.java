// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e.optoutmatrix;

// @jharmonizer:sort-off
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class ZuluImportHelper{static String label(){return "zulu-import";}}
public class FileSortOffBetweenPackageAndImports{int zebra;
    static class ZuluNested{String describe(){return "zulu";}}
    int ant;
    // @jharmonizer:fully-off
    static class InnerFullyOff{int zebra; static class LaterNested{static String describe(){return "later";}} int ant; String describe(){return LaterNested.describe()+zebra+ant;}}
    static class AlphaNested{String describe(){return "alpha";}}
    public static void main(String[] args){System.out.println(new FileSortOffBetweenPackageAndImports().describe());}
    String describe(){List<String> labels=new ArrayList<>(); labels.add(new ZuluNested().describe().toUpperCase(Locale.ROOT)); labels.add(new AlphaNested().describe()); labels.add(new InnerFullyOff().describe()); return String.join("-",labels)+zebra+ant+ZuluImportHelper.label()+BetaImportHelper.label();}
}
class BetaImportHelper{static String label(){return "beta-import";}}
