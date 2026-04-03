// @jharmonizer:sort-off
package io.github.lemon_ant.jharmonizer.core.e2e.optoutmatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class ZetaImportHelper{static String label(){return "zeta-import";}}
public class FileSortOffBeforePackageWithImports{static class ZuluNested{static String describe(){return "zulu";}} int zebra; static class AlphaNested{static String describe(){return "alpha";}} int ant;
    // @jharmonizer:sort-off
    static class InnerSortOff{int zebra; static class ZuluInner{static String describe(){return "zulu-inner";}} int ant; static class AlphaInner{static String describe(){return "alpha-inner";}} String describe(){return ZuluInner.describe()+AlphaInner.describe()+zebra+ant;}
        // @jharmonizer:fully-off
        static class DeepInner{int later; static String describe(){return "deep";} int earlier;}
    }
    public static void main(String[] args){System.out.println(new FileSortOffBeforePackageWithImports().describe());}
    String describe(){List<String> labels=new ArrayList<>(); labels.add(AlphaNested.describe()); labels.add(ZuluNested.describe().toUpperCase(Locale.ROOT)); labels.add(new InnerSortOff().describe()); return String.join("-",labels)+zebra+ant+ZetaImportHelper.label()+BetaImportHelper.label();}
}
class BetaImportHelper{static String label(){return "beta-import";}}
