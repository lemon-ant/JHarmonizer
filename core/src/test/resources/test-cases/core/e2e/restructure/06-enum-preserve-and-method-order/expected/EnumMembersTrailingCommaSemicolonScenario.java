package io.github.lemon_ant.jharmonizer.core.e2e;

public enum EnumMembersTrailingCommaSemicolonScenario {
    GAMMA(3),
    ALPHA(1),
    BETA(2),
    ;

    String aLabel() {
        return name().toLowerCase();
    }

    int bCode() {
        return code;
    }

    static String zUtility() {
        return "utility";
    }

    private final int code;

    private EnumMembersTrailingCommaSemicolonScenario(int code) {
        this.code = code;
    }
}
