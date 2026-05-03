// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
// @jharmonizer:sort-off
class ZuluHelper {
    static String label() {
        return "zulu";
    }
}

public class FileSortOffWithNestedFullyOff {
    int walrus;

    static class ZetaNested {
        String label() {
            return "zeta";
        }
    }

    int aardvark;

    static String suffix() {
        return new BetaNested().label() + new ZetaNested().label() + ZuluHelper.label() + AlphaHelper.label();
    }

    static class BetaNested {
        String label() {
            return "beta";
        }
    }

    public static void main(String[] args) {
        System.out.println(new FileSortOffWithNestedFullyOff().describe());
    }

    String describe() {
        return new Inner().describe() + suffix() + walrus + aardvark;
    }
    // @jharmonizer:fully-off
    static class Inner{int zebra; static class LaterNested{static String describe(){return "later";}} int ant; String describe(){return LaterNested.describe()+zebra+ant;}}
}

class AlphaHelper {
    static String label() {
        return "alpha";
    }
}
