package io.github.lemon_ant.jharmonizer.core.e2e;

public class OuterThisQualifiedNestedClassForwardReferenceSample {
    private final int zDependent = new Inner().captured;
    private int aProvider = 10;

    private final class Inner {
        private final int captured = OuterThisQualifiedNestedClassForwardReferenceSample.this.aProvider + 1;
    }

    private static int aUtility() {
        return 1;
    }

    private static int zUtility() {
        return 2;
    }

    public static void main(String[] args) {
        int utilitiesSum = zUtility() + aUtility();
        OuterThisQualifiedNestedClassForwardReferenceSample sample =
                new OuterThisQualifiedNestedClassForwardReferenceSample();
        if (sample.zDependent != 1 || sample.aProvider != 10 || utilitiesSum != 3) {
            throw new IllegalStateException(
                    "Unexpected values: zDependent="
                            + sample.zDependent
                            + ", aProvider="
                            + sample.aProvider
                            + ", utilitiesSum="
                            + utilitiesSum);
        }
    }
}
