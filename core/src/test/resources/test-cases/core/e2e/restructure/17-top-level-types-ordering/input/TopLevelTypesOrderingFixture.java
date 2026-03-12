enum ZetaKind {
    ONE
}

@interface Marker {}

interface AlphaContract {}

record BravoRecord(int value) {}

class AlphaHelper {
    static String message() {
        return "ok";
    }
}

public class TopLevelTypesOrderingFixture {
    public static void main(String[] args) {
        System.out.println(new BravoRecord(1).value()
                + AlphaHelper.message()
                + AlphaContract.class.getSimpleName()
                + ZetaKind.ONE.name()
                + Marker.class.getSimpleName());
    }
}
