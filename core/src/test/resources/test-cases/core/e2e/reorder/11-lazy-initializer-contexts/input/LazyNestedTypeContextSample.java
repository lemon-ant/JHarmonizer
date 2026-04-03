package io.github.lemon_ant.jharmonizer.core.e2e;

public class LazyNestedTypeContextSample {
    static int zProvider = 7;
    static Runnable aDependent = new Runnable() {
        @Override
        public void run() {
            if (LazyNestedTypeContextSample.zProvider != 7) {
                throw new IllegalStateException(
                        "Unexpected zProvider value in anonymous type: " + LazyNestedTypeContextSample.zProvider);
            }
        }
    };
    static int bIndependent = 3;

    public static void main(String[] args) {
        aDependent.run();
        if (bIndependent != 3 || zProvider != 7) {
            throw new IllegalStateException(
                    "Unexpected values: bIndependent=" + bIndependent + ", zProvider=" + zProvider);
        }
    }
}
