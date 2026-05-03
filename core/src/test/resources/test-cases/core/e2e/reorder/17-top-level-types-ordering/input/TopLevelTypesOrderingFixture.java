/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
public class TopLevelTypesOrderingFixture {
    public static void main(String[] args) {
        System.out.println(new BetaRecord(1).value()
                + AlphaUtility.message()
                + BetaUtility.message()
                + GammaRecord.class.getSimpleName()
                + GammaContract.class.getSimpleName()
                + ZetaKind.ONE.name()
                + AlphaAnnotation.class.getSimpleName()
                + AlphaKind.TWO.name()
                + BetaContract.class.getSimpleName()
                + ZetaAnnotation.class.getSimpleName());
    }
}

enum AlphaKind {
    TWO
}

enum ZetaKind {
    ONE
}

interface BetaContract {
    String value();
}

interface GammaContract {
    String value();
}

class BetaUtility {
    static String message() {
        return "beta";
    }
}

class ZetaUtility {
    static String message() {
        return "zeta";
    }
}

@interface ZetaAnnotation {}

@interface AlphaAnnotation {}

record GammaRecord(String value) {}

record BetaRecord(int value) {}

class AlphaUtility {
    static String message() {
        return "ok";
    }
}
