package io.github.lemon_ant.jharmonizer.core.e2e;

public class DefaultConfigComplexTypesScenario {

    public static void main(String[] args) {
        AlphaRecord record = new AlphaRecord(2, 1);
        if (!"2:1".equals(record.zDescribe())) {
            throw new IllegalStateException("Unexpected record behavior");
        }

        BetaInterface beta = new BetaInterface() {
            @Override
            public String cAbstract() {
                return "abstract";
            }
        };

        if (!"dDefault".equals(beta.dDefault())) {
            throw new IllegalStateException("Unexpected interface default method behavior");
        }

        if (ZetaEnum.values()[0] != ZetaEnum.GAMMA
                || ZetaEnum.values()[1] != ZetaEnum.ALPHA
                || ZetaEnum.values()[2] != ZetaEnum.BETA) {
            throw new IllegalStateException("Enum constants order changed");
        }

        if (!"enum".equals(ZetaEnum.zUtility())) {
            throw new IllegalStateException("Enum utility method failed");
        }

        if (PrivateEnum.FIRST.ordinal() != 1 || PrivateEnum.SECOND.ordinal() != 0) {
            throw new IllegalStateException("Nested enum constants order changed");
        }
    }

    private @interface PrivateAnnotation {

        String name();
    }

    private interface PrivateInterface {

        String value();
    }

    private enum PrivateEnum {
        SECOND,
        FIRST
    }

    private record PrivateRecord(String value) {}
}

record AlphaRecord(int z, int a) {

    public String zDescribe() {
        return z + ":" + a;
    }

    static String zUtility() {
        return "zUtility";
    }

    AlphaRecord {
        if (z < 0 || a < 0) {
            throw new IllegalArgumentException("Record components must be non-negative");
        }
    }

    private static String aPrivateUtility() {
        return "aPrivateUtility";
    }

    private String aInternal() {
        return "internal-" + zDescribe();
    }
}

interface BetaInterface {

    static String bStatic() {
        return "bStatic";
    }

    String cAbstract();

    default String dDefault() {
        return "dDefault";
    }

    private static String zPrivateStatic() {
        return "zPrivateStatic";
    }

    private String aPrivate() {
        return "aPrivate";
    }
}

enum ZetaEnum {
    GAMMA,
    ALPHA,
    BETA;

    private static final String MARKER = "enum";

    private final int code;

    public static String zUtility() {
        return MARKER;
    }

    public int bCode() {
        return code;
    }

    private ZetaEnum() {
        this.code = ordinal();
    }

    private String aLabel() {
        return name().toLowerCase();
    }
}

@interface SampleAnno {

    String alpha() default "alpha";

    int zeta() default 7;
}
