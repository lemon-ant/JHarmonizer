package io.github.lemon_ant.jharmonizer.core.e2e;

interface BetaInterface {
    private static String zPrivateStatic() {
        return "zPrivateStatic";
    }

    static String bStatic() {
        return "bStatic";
    }

    default String dDefault() {
        return "dDefault";
    }

    private String aPrivate() {
        return "aPrivate";
    }

    String cAbstract();
}

@interface SampleAnno {
    int zeta() default 7;

    String alpha() default "alpha";
}

record AlphaRecord(int z, int a) {
    static String zUtility() {
        return "zUtility";
    }

    private static String aPrivateUtility() {
        return "aPrivateUtility";
    }

    AlphaRecord {
        if (z < 0 || a < 0) {
            throw new IllegalArgumentException("Record components must be non-negative");
        }
    }

    public String zDescribe() {
        return z + ":" + a;
    }

    private String aInternal() {
        return "internal-" + zDescribe();
    }
}

enum ZetaEnum {
    GAMMA,
    ALPHA,
    BETA;

    private final int code;

    private static final String MARKER = "enum";

    private ZetaEnum() {
        this.code = ordinal();
    }

    public static String zUtility() {
        return MARKER;
    }

    public int bCode() {
        return code;
    }

    private String aLabel() {
        return name().toLowerCase();
    }
}

public class DefaultConfigComplexTypesScenario {
    private enum PrivateEnum {
        SECOND,
        FIRST
    }

    private interface PrivateInterface {
        String value();
    }

    private @interface PrivateAnnotation {
        String name();
    }

    private record PrivateRecord(String value) {}

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
}
