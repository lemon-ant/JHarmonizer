package io.github.lemon_ant.jharmonizer.core.e2e;

public class ExplicitTypeInstanceReferrerForwardReferenceSample {
    private int zInstance = ExplicitTypeInstanceReferrerForwardReferenceSample.aStatic + 1;
    private static int aStatic = 10;

    public static void main(String[] args) {
        ExplicitTypeInstanceReferrerForwardReferenceSample sample =
                new ExplicitTypeInstanceReferrerForwardReferenceSample();
        if (sample.zInstance != 11 || aStatic != 10) {
            throw new IllegalStateException(
                    "Unexpected values: zInstance=" + sample.zInstance + ", aStatic=" + aStatic);
        }
    }
}
