package e2e;

public class ExplicitTypeInstanceReferrerMissingDependencySample {
    private int zzz = ExplicitTypeInstanceReferrerMissingDependencySample.bravo + 1;
    private static int bravo = 10;

    public static void main(String[] args) {
        ExplicitTypeInstanceReferrerMissingDependencySample sample =
                new ExplicitTypeInstanceReferrerMissingDependencySample();
        if (sample.zzz != 1 || bravo != 10) {
            throw new IllegalStateException(
                    "Unexpected values: zzz=" + sample.zzz + ", bravo=" + bravo);
        }
    }
}
