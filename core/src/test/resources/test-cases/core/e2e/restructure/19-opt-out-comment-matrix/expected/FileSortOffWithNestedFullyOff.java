// @jharmonizer:sort-off
public class FileSortOffWithNestedFullyOff {
    int walrus;
    int aardvark;

    public static void main(String[] args) {
        System.out.println(new FileSortOffWithNestedFullyOff().describe());
    }

    String describe() {
        return new Inner().describe() + walrus + aardvark;
    }
    // @jharmonizer:fully-off
    static class Inner{String describe(){return "inner";}}
}

class AlphaHelper {}
