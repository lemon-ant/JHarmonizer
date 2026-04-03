// @jharmonizer:fully-off
class ZetaTopLevel{static String label(){return "zeta";}}
public class FileFullyOffWithTopLevelAndNestedOverrides{static class ZuluNested{static String label(){return "zulu";}} int zebra; static class AlphaNested{static String label(){return "alpha";}} int ant;
    // @jharmonizer:sort-off
    static class InnerSortOff{int zebra; static class ZuluInner{static String describe(){return "zulu-inner";}} int ant; static class AlphaInner{static String describe(){return "alpha-inner";}} String describe(){return ZuluInner.describe()+AlphaInner.describe()+zebra+ant;}}
    // @jharmonizer:fully-off
    static class InnerFullyOff{int zebra; static class LaterInner{static String describe(){return "later-inner";}} int ant; String describe(){return LaterInner.describe()+zebra+ant;}}
    public static void main(String[] args){System.out.println(new FileFullyOffWithTopLevelAndNestedOverrides().describe());}
    String describe(){return new ZuluNested().label()+new AlphaNested().label()+new InnerSortOff().describe()+new InnerFullyOff().describe()+zebra+ant+ZetaTopLevel.label()+BetaTopLevel.label();}
}
class BetaTopLevel{static String label(){return "beta";}}
