package io.github.lemon_ant.jharmonizer.core.e2e;

public enum EnumMembersSemicolonScenario {
    GAMMA(3),
    ALPHA(1),
    BETA(2);

    private final int code;

    private EnumMembersSemicolonScenario(int code) {
        this.code = code;
    }

    static String zUtility() {
        return "utility";
    }

    int bCode() {
        return code;
    }

    String aLabel() {
        return name().toLowerCase();
    }
}
