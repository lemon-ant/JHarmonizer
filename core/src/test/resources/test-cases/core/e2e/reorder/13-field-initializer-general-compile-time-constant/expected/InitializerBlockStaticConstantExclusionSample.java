package io.github.lemon_ant.jharmonizer.core.e2e;

public class InitializerBlockStaticConstantExclusionSample {
    static int bProvider = Integer.parseInt("1");

    static {
        sink = InitializerBlockStaticConstantExclusionSample.bProvider
                + InitializerBlockStaticConstantExclusionSample.zConstant;
    }

    static int sink;
    static final int zConstant = 7;

    public static void main(String[] args) {
        if (bProvider != 1 || zConstant != 7 || sink != 8) {
            throw new IllegalStateException(
                    "Unexpected values: bProvider=" + bProvider + ", zConstant=" + zConstant + ", sink=" + sink);
        }
    }
}
