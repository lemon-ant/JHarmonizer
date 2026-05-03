// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
class Gamma{int gammaLast;
    static String middle(){return "gamma-middle";}
    int gammaFirst;
    static String describe(){return "gamma";}
}

/* @jharmonizer:fully-off */
class Beta {
    int betaLast;   int betaFirst;
    static class BetaNested{static String describe(){return "beta-nested";}int later; int earlier;}

    static String describe() {
        return BetaNested.describe() + "beta";
    }
}

public class NestedOptOutCombination {
    int walrus;
    static class ZetaSortableInner{static String describe(){return "zeta";} int omega; int alpha;}
    int aardvark;
    static class AlphaSortableInner{static String describe(){return "alpha";} int beta; int aardvark;}

    public static void main(String[] args) {
        System.out.println(new NestedOptOutCombination().describe());
    }

    static String extra() {
        return new ZetaSortableInner().describe() + new AlphaSortableInner().describe();
    }

    String describe() {
        return Beta.describe() + Gamma.describe() + Inner.describe() + extra() + aardvark + walrus;
    }

    // @jharmonizer:sort-off
    static class Inner {
        int zebra;
        static class ZuluNested{static String describe(){return "zulu";}}
        int ant;

        static String describe() {
            return DeepInner.describe() + new ZuluNested().describe() + new AlphaNested().describe();
        }

        static class AlphaNested{static String describe(){return "alpha";}}

        // @jharmonizer:fully-off
        static class DeepInner{int later; static String describe(){return "deep";} int earlier;}
    }
}
