
/* @jharmonizer:fully-off */
class Beta {
    int betaLast;   int betaFirst;
    static class BetaNested{static String describe(){return "beta-nested";}int later; int earlier;}

    static String describe() {
        return BetaNested.describe() + "beta";
    }
}

class Gamma {

    static String describe() {
        return "gamma";
    }

    int gammaFirst;
    int gammaLast;

    static String middle() {
        return "gamma-middle";
    }
}

public class NestedOptOutCombination {

    static class AlphaSortableInner {

        int aardvark;
        int beta;

        static String describe() {
            return "alpha";
        }
    }

    // @jharmonizer:sort-off
    static class Inner {
        int zebra;

        static class ZuluNested {
            static String describe() {
                return "zulu";
            }
        }

        int ant;

        static String describe() {
            return DeepInner.describe() + new ZuluNested().describe() + new AlphaNested().describe();
        }

        static class AlphaNested {
            static String describe() {
                return "alpha";
            }
        }

        // @jharmonizer:fully-off
        static class DeepInner {
            int later;

            static String describe() {
                return "deep";
            }

            int earlier;
        }
    }

    static class ZetaSortableInner {

        int alpha;

        static String describe() {
            return "zeta";
        }

        int omega;
    }

    int aardvark;

    String describe() {
        return Beta.describe() + Gamma.describe() + Inner.describe() + extra() + aardvark + walrus;
    }

    static String extra() {
        return new ZetaSortableInner().describe() + new AlphaSortableInner().describe();
    }

    public static void main(String[] args) {
        System.out.println(new NestedOptOutCombination().describe());
    }

    int walrus;
}
