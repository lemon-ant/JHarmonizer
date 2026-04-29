package io.github.lemon_ant.jharmonizer.core.e2e;

public enum EnumMembersSemicolonScenario {
    GAMMA(3),
    ALPHA(1),
    BETA(2);

    String aLabel() {
        return name().toLowerCase();
    }

    int bCode() {
        return code;
    }

    public static void main(String[] args) {
        if (GAMMA.bCode() != 3 || ALPHA.bCode() != 1 || BETA.bCode() != 2) {
            throw new IllegalStateException("Unexpected enum code values:"
                    + " GAMMA=" + GAMMA.bCode()
                    + ", ALPHA=" + ALPHA.bCode()
                    + ", BETA=" + BETA.bCode());
        }
    }

    static String zUtility() {
        return "utility";
    }

    private final int code;

    private EnumMembersSemicolonScenario(int code) {
        this.code = code;
    }
}
