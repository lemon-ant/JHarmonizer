package io.github.lemon_ant.jharmonizer.core.e2e;

public class OuterThisQualifiedNestedClassForwardReferenceSample {

    private int aProvider = 10;
    private static final int aUtilityConstant = 1;
    private final int zDependent = new Inner().captured;
    private static final int zUtilityConstant = 2;

    private final class Inner {

        private final int captured = OuterThisQualifiedNestedClassForwardReferenceSample.this.aProvider + 1;
    }

    public static void main(String[] args) {
        int utilitiesSum = zUtilityConstant + aUtilityConstant;
        OuterThisQualifiedNestedClassForwardReferenceSample sample =
                new OuterThisQualifiedNestedClassForwardReferenceSample();
        if (sample.zDependent != 11 || sample.aProvider != 10 || utilitiesSum != 3) {
            throw new IllegalStateException("Unexpected values: zDependent="
                    + sample.zDependent
                    + ", aProvider="
                    + sample.aProvider
                    + ", utilitiesSum="
                    + utilitiesSum);
        }
    }
}
