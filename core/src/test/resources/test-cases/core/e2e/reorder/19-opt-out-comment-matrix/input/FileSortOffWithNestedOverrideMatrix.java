// @jharmonizer:sort-off
class ZuluHelper{static String label(){return "zulu";}}
public class FileSortOffWithNestedOverrideMatrix{int zebra;
    static class LaterOuter{static String label(){return "later";}}
    int ant;
    // @jharmonizer:sort-off
    static class InnerSortOff{int zebra; static class ZuluNested{static String describe(){return "zulu";}} int ant; static class AlphaNested{static String describe(){return "alpha";}}
        static String describe(){return DeepInner.describe()+new ZuluNested().describe()+new AlphaNested().describe();}
        // @jharmonizer:fully-off
        static class DeepInner{int later; static String describe(){return "deep";} int earlier;}
    }
    // @jharmonizer:fully-off
    static class InnerFullyOff{int zebra; static class LaterNested{static String describe(){return "later";}} int ant; String describe(){return LaterNested.describe()+zebra+ant;}}
    public static void main(String[] args){
        System.out.println(new FileSortOffWithNestedOverrideMatrix().describe());
    }
    String describe(){
        return new InnerSortOff().describe()+new InnerFullyOff().describe()+LaterOuter.label()+zebra+ant+ZuluHelper.label()+AlphaHelper.label();
    }
}
class AlphaHelper{static String label(){return "alpha";}}
