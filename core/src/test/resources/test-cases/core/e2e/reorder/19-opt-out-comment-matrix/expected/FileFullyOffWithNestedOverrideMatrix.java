// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
// @jharmonizer:fully-off
class ZuluHelper{static String label(){return "zulu";}}
public class FileFullyOffWithNestedOverrideMatrix{int zebra;
    static class LaterOuter{static String label(){return "later";}}
    int ant;
    // @jharmonizer:sort-off
    static class InnerSortOff{int zebra; static class BetaNested{static String describe(){return "beta";}} int ant; static class AlphaNested{static String describe(){return "alpha";}} String describe(){return BetaNested.describe()+new AlphaNested().describe()+zebra+ant;}}
    // @jharmonizer:fully-off
    static class InnerFullyOff{int zebra; static class LaterNested{static String describe(){return "later";}} int ant; String describe(){return LaterNested.describe()+zebra+ant;}}
    public static void main(String[] args){
        System.out.println(new FileFullyOffWithNestedOverrideMatrix().describe());
    }
    String describe(){
        return new InnerSortOff().describe()+new InnerFullyOff().describe()+LaterOuter.label()+zebra+ant+ZuluHelper.label()+AlphaHelper.label();
    }
}
class AlphaHelper{static String label(){return "alpha";}}
