package io.github.lemon_ant.jharmonizer.core.e2e;

// Verifies that write-only (LHS) field access in initializer does not create read-like declaration dependency.
public class WriteOnlyQualifiedAssignmentPendingSample {
    static int zTarget;
    static int bIndependent = 1;
    static int aWriter = (WriteOnlyQualifiedAssignmentPendingSample.zTarget = 5);

    public static void main(String[] args) {
        if (aWriter != 5 || bIndependent != 1 || zTarget != 5) {
            throw new IllegalStateException("Unexpected values: aWriter=" + aWriter
                    + ", bIndependent=" + bIndependent
                    + ", zTarget=" + zTarget);
        }
    }
}
