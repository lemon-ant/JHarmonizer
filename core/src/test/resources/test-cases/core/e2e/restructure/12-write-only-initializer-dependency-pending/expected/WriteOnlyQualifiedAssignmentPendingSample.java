package io.github.lemon_ant.jharmonizer.core.e2e;

// Expected: alpha ordering is preserved for fields because pure LHS assignment is not treated as read dependency.
public class WriteOnlyQualifiedAssignmentPendingSample {
    static int aWriter = (WriteOnlyQualifiedAssignmentPendingSample.zTarget = 5);
    static int bIndependent = 1;
    static int zTarget;

    public static void main(String[] args) {
        if (aWriter != 5 || bIndependent != 1 || zTarget != 5) {
            throw new IllegalStateException("Unexpected values: aWriter=" + aWriter
                    + ", bIndependent=" + bIndependent
                    + ", zTarget=" + zTarget);
        }
    }
}
