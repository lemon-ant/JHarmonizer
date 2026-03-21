package nested.deeper;

public class NestedSortOffWithDeepFullyOff {
    int aardvark;

    String describe() {
        return Inner.describe() + aardvark + walrus;
    }

    public static void main(String[] args) {
        System.out.println(new NestedSortOffWithDeepFullyOff().describe());
    }

    // @jharmonizer:sort-off
    static class Inner {
        int zebra;
        int ant;

        // @jharmonizer:fully-off
        static class DeepInner{static String describe(){return "deep";}int later;  int earlier;}

        static String describe() {
            return DeepInner.describe();
        }
    }

    int walrus;
}
