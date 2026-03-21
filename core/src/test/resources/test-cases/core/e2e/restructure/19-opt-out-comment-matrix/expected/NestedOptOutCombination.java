
/* @jharmonizer:fully-off */
class Beta {
    int betaLast;   int betaFirst;

    static String describe() {
        return "beta";
    }
}

class Gamma {
    static String describe() {
        return "gamma";
    }

    int gammaFirst;
    int gammaLast;
}

public class NestedOptOutCombination {

    // @jharmonizer:sort-off
    static class Inner {
        int zebra;
        int ant;

        static String describe() {
            return DeepInner.describe();
        }

        // @jharmonizer:fully-off
        static class DeepInner {
            static String describe() {
                return "deep";
            }

            int later;
            int earlier;
        }
    }

    int aardvark;

    String describe() {
        return Beta.describe() + Gamma.describe() + Inner.describe() + aardvark + walrus;
    }

    public static void main(String[] args) {
        System.out.println(new NestedOptOutCombination().describe());
    }

    int walrus;
}
