package e2e;

public class FieldInitializerExplicitDeclaringTypeFinalConstantVsNonConstantSample {
    private static final int anchor = 100;
    private static final int constValue = 41;
    private static final int fromConst =
            FieldInitializerExplicitDeclaringTypeFinalConstantVsNonConstantSample.constValue + 1;
    private static final int fromNonConst =
            FieldInitializerExplicitDeclaringTypeFinalConstantVsNonConstantSample.nonConstValue + 1;
    private static final int nonConstValue = Integer.parseInt("41");

    public static void main(String[] args) {
        if (anchor != 100 || constValue != 41 || nonConstValue != 41 || fromConst != 42 || fromNonConst != 1) {
            throw new IllegalStateException("Unexpected initialization values:"
                    + " anchor="
                    + anchor
                    + ", constValue="
                    + constValue
                    + ", nonConstValue="
                    + nonConstValue
                    + ", fromConst="
                    + fromConst
                    + ", fromNonConst="
                    + fromNonConst);
        }
    }
}
