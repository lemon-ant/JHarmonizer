package io.github.lemon_ant.jharmonizer.core.e2e;

/**
 * Exercises CtEnumValue filtering in dependency graph providers.
 *
 * <p>Enum constants are CtField subtypes (CtEnumValue extends CtField in Spoon).
 * Without explicit filtering, field-oriented dependency providers would treat
 * enum constants as regular fields, creating spurious dependency edges.
 *
 * <p>This fixture combines:
 * <ul>
 *   <li>Enum constants with non-trivial initializers referencing static fields
 *       (exercises streamFieldAccessesInSameType filter — enum constants must not
 *       become providers via field access)</li>
 *   <li>Static fields with backward-reference initializer chains
 *       (exercises FieldInitializerBackwardReferenceDependencyProvider filter —
 *       enum constants must not be scanned as dependent fields)</li>
 *   <li>Static fields with explicit {@code EnumType.field} forward references
 *       (exercises AbstractExplicitInitializerForwardReferenceDependencyProvider
 *       findDirectProviderEdges and findEarlierReferrerFieldsWithExplicitReferenceTo
 *       filters — enum constants must not appear as dependent or referrer fields)</li>
 *   <li>Blank final static field assigned in a static initializer block
 *       (exercises InitializationOrderDependencyUtils matchesInitializationMemberStaticness
 *       and resolveInitializationAstRoot filters — enum constants must not be treated
 *       as initialization members for blank final analysis)</li>
 * </ul>
 */
public enum EnumFieldDependencyFilteringScenario {
    SECOND("second"),
    FIRST("first");

    private static String zStaticProvider = "provider";

    private final String label;

    private static final String ySnapshot = FIRST.label + "+" + SECOND.label;

    private static final String xDependent = zStaticProvider + "-chain";

    private static final String wDoubleChain = xDependent + "-end";

    private static final String vExplicitRef =
            EnumFieldDependencyFilteringScenario.zStaticProvider + "-explicit";

    private static final String uBlankFinal;

    static {
        uBlankFinal = zStaticProvider + "-blank";
    }

    private EnumFieldDependencyFilteringScenario(String label) {
        this.label = label;
    }

    public static void main(String[] args) {
        if (!"first+second".equals(ySnapshot)) {
            throw new IllegalStateException("ySnapshot=" + ySnapshot);
        }
        if (!"provider-chain".equals(xDependent)) {
            throw new IllegalStateException("xDependent=" + xDependent);
        }
        if (!"provider-chain-end".equals(wDoubleChain)) {
            throw new IllegalStateException("wDoubleChain=" + wDoubleChain);
        }
        if (!"provider-explicit".equals(vExplicitRef)) {
            throw new IllegalStateException("vExplicitRef=" + vExplicitRef);
        }
        if (!"provider-blank".equals(uBlankFinal)) {
            throw new IllegalStateException("uBlankFinal=" + uBlankFinal);
        }
    }
}
