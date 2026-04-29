/* @jharmonizer:fully-off */
package io.github.lemon_ant.jharmonizer.core.e2e.optoutmatrix;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

class ZetaPackageHelper{static String label(){return "zeta-package";}}
public class FileFullyOffBlockBeforePackageWithImports{static class ZuluNested{static String describe(){return "zulu";}} int zebra; static class AlphaNested{static String describe(){return "alpha";}} int ant;
    // @jharmonizer:fully-off
    static class InnerFullyOff{int zebra; static class LaterNested{static String describe(){return "later";}} int ant; String describe(){return LaterNested.describe()+zebra+ant;}}
    // @jharmonizer:sort-off
    static class InnerSortOff{int zebra; static class ZuluInner{static String describe(){return "zulu-inner";}} int ant; static class AlphaInner{static String describe(){return "alpha-inner";}} String describe(){return ZuluInner.describe()+AlphaInner.describe()+zebra+ant;}}
    public static void main(String[] args){System.out.println(new FileFullyOffBlockBeforePackageWithImports().describe());}
    String describe(){List<String> labels=new LinkedList<>(); labels.add(AlphaNested.describe()); labels.add(ZuluNested.describe().toUpperCase(Locale.ROOT)); labels.add(new InnerFullyOff().describe()); labels.add(new InnerSortOff().describe()); return String.join("-",labels)+zebra+ant+ZetaPackageHelper.label()+BetaPackageHelper.label();}
}
class BetaPackageHelper{static String label(){return "beta-package";}}
