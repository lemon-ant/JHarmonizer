package io.github.lemon_ant.jharmonizer.core.config.compiled;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Utilities for encoding member declaration flags into a single 32-bit integer mask
 * and performing common mask checks.
 *
 * <p>The mask consists of three independent bit segments (from least to most significant bits):</p>
 *
 * <pre>
 * [ MODIFIERS ][ ACCESS ][ KIND ]   // LSB → MSB
 * </pre>
 *
 * <ul>
 *   <li><b>MODIFIERS</b> — multi-bit subset (one bit per DeclarationModifier)</li>
 *   <li><b>ACCESS</b>    — one-hot (one bit per MemberAccess); may be 0 when access is not applicable</li>
 *   <li><b>KIND</b>      — one-hot (one bit per MemberKind)</li>
 * </ul>
 *
 * <p>Offsets are computed from enum sizes:</p>
 * <pre>
 * MODIFIER_OFFSET = 0
 * ACCESS_OFFSET   = MODIFIER_OFFSET + MODIFIER_COUNT
 * KIND_OFFSET     = ACCESS_OFFSET   + ACCESS_COUNT
 * </pre>
 *
 * <p>Total bits = MODIFIER_COUNT + ACCESS_COUNT + KIND_COUNT (must be ≤ 32).
 * If it exceeds 32, an IllegalStateException should be thrown by the encoder and the
 * implementation should migrate to a 64-bit mask.</p>
 */
@UtilityClass
public class MemberDeclarationFlagsUtil {

    private static final int DECLARATION_MODIFIER_COUNT = DeclarationModifier.values().length;
    private static final int MEMBER_KIND_COUNT = MemberKind.values().length;
    private static final int MEMBER_ACCESS_COUNT = MemberAccess.values().length;
    private static final int DECLARATION_MODIFIER_OFFSET = 0;
    private static final int MEMBER_ACCESS_OFFSET = DECLARATION_MODIFIER_OFFSET + DECLARATION_MODIFIER_COUNT;
    private static final int MEMBER_KIND_OFFSET = MEMBER_ACCESS_OFFSET + MEMBER_ACCESS_COUNT;

    private static final int TOTAL_FEATURE_BITS = DECLARATION_MODIFIER_COUNT + MEMBER_ACCESS_COUNT + MEMBER_KIND_COUNT;
    private static final int INTEGER_BITS_COUNT = 32;

    static {
        if (TOTAL_FEATURE_BITS > INTEGER_BITS_COUNT) {
            throw new IllegalStateException("Feature mask does not fit into 32 bits (" + TOTAL_FEATURE_BITS
                    + " bits). Migrate mask type to long before adding new enum constants.");
        }
    }

    /**
     * Encodes member declaration flags into a single 32-bit mask.
     *
     * <p>The resulting mask is the bitwise OR of the three segments:</p>
     * <ul>
     *   <li>KIND one-hot bit (exactly one bit set),</li>
     *   <li>ACCESS one-hot bit (zero or one bit set; zero if {@code memberAccess} is not applicable),</li>
     *   <li>MODIFIERS subset bits (zero or more bits set).</li>
     * </ul>
     *
     * @param memberKind           the member kind to encode; must not be {@code null}. Exactly one KIND bit is set.
     * @param memberAccess         the member access to encode; may be {@code null} when access is not applicable
     *                             (e.g., for initializer blocks). If {@code null}, no ACCESS bit is set.
     * @param declarationModifiers the set of declaration modifiers to encode; must not be {@code null}. May be empty.
     *                             Each modifier contributes one bit in the MODIFIERS segment.
     * @return a 32-bit integer mask representing the provided member declaration flags
     * @implNote Bit positions are derived from enum {@code ordinal()} values and contiguous
     * segment offsets; changing enum order will change bit layout. Tests should guard
     * the expected layout and non-overlapping segments.
     */
    public static int encodeMemberDeclarationFlags(
            @NonNull MemberKind memberKind,
            @Nullable MemberAccess memberAccess,
            @NonNull Set<DeclarationModifier> declarationModifiers) {
        return encodeMemberKindToFlag(memberKind)
                | encodeMemberAccessToFlag(memberAccess)
                | encodeDeclarationModifiersToFlags(declarationModifiers);
    }

    /**
     * Encodes multiple declaration attributes into a single bit mask.
     * Combines flags for kinds, access levels, and modifiers using bitwise OR.
     * Empty sets contribute 0 (i.e., no requirement for that category).
     *
     * @param memberKinds          non-null set of kinds to encode (may be empty)
     * @param memberAccesses       non-null set of access levels to encode (may be empty)
     * @param declarationModifiers non-null set of modifiers to encode (may be empty)
     * @return bit mask equal to OR of all encoded kind, access, and modifier flags
     */
    public static int encodeMemberDeclarationFlags(
            @NonNull Set<MemberKind> memberKinds,
            @NonNull Set<MemberAccess> memberAccesses,
            @NonNull Set<DeclarationModifier> declarationModifiers) {

        int kindFlags = memberKinds.stream()
                .mapToInt(MemberDeclarationFlagsUtil::encodeMemberKindToFlag)
                .reduce(0, (a, b) -> a | b);

        int accessFlags = memberAccesses.stream()
                .mapToInt(MemberDeclarationFlagsUtil::encodeMemberAccessToFlag)
                .reduce(0, (a, b) -> a | b);

        int modifierFlags = encodeDeclarationModifiersToFlags(declarationModifiers);

        return kindFlags | accessFlags | modifierFlags;
    }

    /**
     * Returns {@code true} if the given member declaration flags mask contains
     * all bits set in the required declaration flags mask.
     *
     * <p>This check is segment-agnostic: any bits present in
     * {@code requiredDeclarationFlags} must also be set in {@code memberDeclarationFlags}.</p>
     *
     * @param memberDeclarationFlags   the full mask computed for a member (KIND, ACCESS and MODIFIERS segments)
     * @param requiredDeclarationFlags the mask that encodes required flags (may include bits from any segment);
     *                                 use zero to indicate “no requirement”
     * @return {@code true} if all required bits are present; {@code false} otherwise
     */
    public static boolean containsAllRequiredDeclarationFlags(
            int memberDeclarationFlags, int requiredDeclarationFlags) {
        return (memberDeclarationFlags & requiredDeclarationFlags) == requiredDeclarationFlags;
    }

    /**
     * Encode a single access to its one-hot bit (null → 0).
     */
    private static int encodeMemberAccessToFlag(MemberAccess memberAccessOrNull) {
        return memberAccessOrNull == null ? 0 : (1 << (MEMBER_ACCESS_OFFSET + memberAccessOrNull.ordinal()));
    }

    /**
     * Encode a single kind to its one-hot bit.
     */
    private static int encodeMemberKindToFlag(MemberKind memberKind) {
        return 1 << (MEMBER_KIND_OFFSET + memberKind.ordinal());
    }

    /**
     * Encode a set of declaration modifiers to a multi-bit subset mask.
     */
    private static int encodeDeclarationModifiersToFlags(Set<DeclarationModifier> declarationModifiers) {
        int mask = 0;
        for (DeclarationModifier modifier : declarationModifiers) {
            mask |= 1 << (DECLARATION_MODIFIER_OFFSET + modifier.ordinal());
        }
        return mask;
    }
}
