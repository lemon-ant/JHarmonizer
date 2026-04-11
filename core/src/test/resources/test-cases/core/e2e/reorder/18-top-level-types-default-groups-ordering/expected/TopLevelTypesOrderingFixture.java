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

class AlphaUtility {

    static String message() {
        return "ok";
    }
}

record BetaRecord(int value) {}

class BetaUtility {

    static String message() {
        return "beta";
    }
}

record GammaRecord(String value) {}

class ZetaUtility {

    static String message() {
        return "zeta";
    }
}

interface BetaContract {

    String value();
}

interface GammaContract {

    String value();
}

enum AlphaKind {
    TWO
}

enum ZetaKind {
    ONE
}

@interface AlphaAnnotation {}

@interface ZetaAnnotation {}
