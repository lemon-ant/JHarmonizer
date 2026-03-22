package io.github.lemon_ant.jharmonizer.core.e2e.optoutmatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// This floating directive is intentionally left orphaned between imports and the first declared type.
// Current behavior is expected to attach it to the next type instead of treating it as file-scope.

// @jharmonizer:sort-off

public class FileSortOffFloatingAfterImportsAttachesToFirstType{int zebra;
    static class ZuluNested{String describe(){return "zulu";}}
    int ant;
    static class AlphaNested{String describe(){return "alpha";}}
    public static void main(String[] args){System.out.println(new FileSortOffFloatingAfterImportsAttachesToFirstType().describe());}
    String describe(){List<String> labels=new ArrayList<>(); labels.add(new ZuluNested().describe().toUpperCase(Locale.ROOT)); labels.add(new AlphaNested().describe()); return String.join("-",labels)+zebra+ant+AlphaAttachmentHelper.label();}
}
class AlphaAttachmentHelper{static String label(){return "alpha-helper";}}
