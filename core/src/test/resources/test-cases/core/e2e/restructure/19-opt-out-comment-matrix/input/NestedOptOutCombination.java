class Gamma{int gammaLast;int gammaFirst;
    static String describe(){return "gamma";}
}

/* @jharmonizer:fully-off */
class Beta {
    int betaLast;   int betaFirst;

    static String describe() {
        return "beta";
    }
}

public class NestedOptOutCombination {
    int walrus;
    int aardvark;

    public static void main(String[] args) {
        System.out.println(new NestedOptOutCombination().describe());
    }

    String describe() {
        return Beta.describe() + Gamma.describe() + Inner.describe() + aardvark + walrus;
    }

    // @jharmonizer:sort-off
    static class Inner {
        int zebra;
        int ant;

        static String describe() {
            return DeepInner.describe();
        }

        // @jharmonizer:fully-off
        static class DeepInner{static String describe(){return "deep";}int later;  int earlier;}
    }
}
