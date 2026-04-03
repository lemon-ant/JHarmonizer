package io.github.lemon_ant.jharmonizer.core.e2e.optoutmatrix;

import java.util.ArrayList;
import java.util.List;

class ZuluTopLevel {
    int zebra;
    static String describe(){return "zulu";}
    int ant;
}

// This floating directive is intentionally left orphaned between top-level types.
// Current behavior is expected to attach it to the next top-level type only.

/* @jharmonizer:fully-off */

class BetaTopLevel{int zebra; static class ZuluNested{static String describe(){return "zulu-nested";}} int ant; static class AlphaNested{static String describe(){return "alpha-nested";}} String describe(){return ZuluNested.describe()+AlphaNested.describe()+zebra+ant;}}

public class FloatingFullyOffBetweenTopLevelTypes {
    public static void main(String[] args) {
        System.out.println(new FloatingFullyOffBetweenTopLevelTypes().describe());
    }

    String describe() {
        List<String> labels = new ArrayList<>();
        labels.add(new ZuluTopLevel().describe());
        labels.add(new BetaTopLevel().describe());
        labels.add(AlphaTopLevel.describe());
        return String.join("-", labels);
    }
}

class AlphaTopLevel {
    int zebra;
    static String describe(){return "alpha";}
    int ant;
}
