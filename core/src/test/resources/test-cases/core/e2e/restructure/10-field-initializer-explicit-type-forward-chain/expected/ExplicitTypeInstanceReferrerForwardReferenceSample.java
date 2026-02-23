package e2e;

public class ExplicitTypeInstanceReferrerForwardReferenceSample {
    private static int aStatic = 10;
    private int zInstance = ExplicitTypeInstanceReferrerForwardReferenceSample.aStatic + 1;

    public static void main(String[] args) {
        ExplicitTypeInstanceReferrerForwardReferenceSample sample =
                new ExplicitTypeInstanceReferrerForwardReferenceSample();
        if (sample.zInstance != 11 || aStatic != 10) {
            throw new IllegalStateException("Unexpected values: zInstance=" + sample.zInstance + ", aStatic=" + aStatic);
        }
    }
}
