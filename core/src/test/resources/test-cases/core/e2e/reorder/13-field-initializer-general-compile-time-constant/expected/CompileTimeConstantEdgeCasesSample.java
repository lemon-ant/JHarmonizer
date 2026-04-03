package io.github.lemon_ant.jharmonizer.core.e2e;

public class CompileTimeConstantEdgeCasesSample {
    static final int bDependsOnCastConstant = CompileTimeConstantEdgeCasesSample.zCastConstant + 10;
    static final String cDependsOnConcatConstant = CompileTimeConstantEdgeCasesSample.yConcatConstant + "!";
    static final String yConcatConstant = "ab" + "cd";
    static final int zCastConstant = (int) (1L + 2L);

    public static void main(String[] args) {
        if (bDependsOnCastConstant != 13
                || !"abcd!".equals(cDependsOnConcatConstant)
                || zCastConstant != 3
                || !"abcd".equals(yConcatConstant)) {
            throw new IllegalStateException("Unexpected values: bDependsOnCastConstant=" + bDependsOnCastConstant
                    + ", cDependsOnConcatConstant=" + cDependsOnConcatConstant + ", zCastConstant="
                    + zCastConstant + ", yConcatConstant=" + yConcatConstant);
        }
    }
}
