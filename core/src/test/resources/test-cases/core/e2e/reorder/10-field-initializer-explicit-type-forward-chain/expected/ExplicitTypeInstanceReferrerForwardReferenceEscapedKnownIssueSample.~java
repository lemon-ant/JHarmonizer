package e2e;

public class ExplicitTypeInstanceReferrerForwardReferenceSample {
    private int aaa = ExplicitTypeInstanceReferrerForwardReferenceSample.bravo + 1;
    private static int zzz = new ExplicitTypeInstanceReferrerForwardReferenceSample().aaa;
    private static int bravo = 10;

    public static void main(String[] args) {
        if (zzz != 1 || bravo != 10) {
            throw new IllegalStateException("Unexpected values: zzz=" + zzz + ", bravo=" + bravo);
        }
    }
}
