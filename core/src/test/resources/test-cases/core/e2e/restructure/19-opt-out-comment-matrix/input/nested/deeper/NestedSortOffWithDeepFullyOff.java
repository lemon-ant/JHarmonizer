package nested.deeper;

public class NestedSortOffWithDeepFullyOff {
    int walrus;
    int aardvark;

    public static void main(String[] args) {
        System.out.println(new NestedSortOffWithDeepFullyOff().describe());
    }

    String describe() {
        return Inner.describe() + aardvark + walrus;
    }

    // @jharmonizer:sort-off
    static class Inner{int zebra;int ant;
        static String describe(){return DeepInner.describe();}
        // @jharmonizer:fully-off
        static class DeepInner{static String describe(){return "deep";}int later;  int earlier;}
    }
}
