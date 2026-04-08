package io.github.lemon_ant.jharmonizer.core.e2e;

public enum EnumStaticMixedInitializationScenario {
    SECOND("second"),
    FIRST("first");

    private static final String bDependent = resolveDependent();
    private final String label;
    private static final String aEnumSnapshot = FIRST.label + ":" + SECOND.label;

    private static String zProvider = "provider";

    public static void main(String[] args) {
        if (!"null-dep".equals(bDependent)) {
            throw new IllegalStateException("Mixed static initialization order changed: " + bDependent);
        }
        if (!"first:second".equals(aEnumSnapshot)) {
            throw new IllegalStateException("Enum constant snapshot changed: " + aEnumSnapshot);
        }
    }

    private static String resolveDependent() {
        return zProvider + "-dep";
    }

    private EnumStaticMixedInitializationScenario(String label) {
        this.label = label;
    }
}
