enum ZetaKind {
    ONE
}

interface GammaContract {}

public class SingleUnifiedGroupFixture {
    public static void main(String[] args) {
        System.out.println(new BetaRecord(1).value()
                + AlphaUtility.message()
                + GammaContract.class.getSimpleName()
                + ZetaKind.ONE.name()
                + AlphaAnnotation.class.getSimpleName());
    }
}

@interface AlphaAnnotation {}

record BetaRecord(int value) {}

class AlphaUtility {
    static String message() {
        return "ok";
    }
}
