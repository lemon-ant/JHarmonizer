package io.github.lemon_ant.jharmonizer.core.config.effective;

import static java.util.Collections.unmodifiableSet;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.Value;

/**
 * Java AST parser-agnostic descriptor for selection and sorting rules.
 * Unifies members and nested types via MemberKind; access level and declaration modifiers
 * are captured explicitly to drive selectors and ordering logic.
 */
@Value
@Builder
@SuppressWarnings("PMD.TooManyMethods")
public class EffectiveMemberDescriptor {

    // Kinds that must not declare any modifiers at all.
    private static final Set<MemberKind> KINDS_WITHOUT_MODIFIERS = EnumSet.of(
            MemberKind.CONSTRUCTOR,
            MemberKind.INIT_BLOCK_STATIC,
            MemberKind.INIT_BLOCK_INSTANCE,
            MemberKind.ENUM_CONSTANT,
            MemberKind.RECORD_COMPONENT);

    /**
     * Simple name (null for initializer blocks).
     */
    @Nullable
    String name;

    /**
     * Unified kind: fields, methods, ctors, init blocks, enum consts, record components, and nested types.
     */
    @NonNull
    MemberKind memberKind;

    /**
     * Access level (PACKAGE means no explicit modifier).
     * For kinds where access is not applicable (see {@link MemberKind#isAccessLevelApplicable()} ()}), this must be null.
     */
    @Nullable
    MemberAccess memberAccess;

    /**
     * Unified set of declaration modifiers (STATIC, FINAL, ABSTRACT, DEFAULT, SEALED, NON_SEALED, etc.).
     */
    @NonNull
    @Singular
    Set<@NonNull DeclarationModifier> declarationModifiers;

    /**
     * Fully-qualified annotation names.
     */
    @NonNull
    @Singular
    Set<@NonNull String> annotationQualifiedNames;

    private EffectiveMemberDescriptor(
            @Nullable String name,
            @NonNull MemberKind memberKind,
            @Nullable MemberAccess memberAccess,
            @NonNull Set<@NonNull DeclarationModifier> declarationModifiers,
            @NonNull Set<@NonNull String> annotationQualifiedNames) {

        this.name = validateAndNormalizeName(name, memberKind);

        // --- access invariants
        validateAccessForMemberKind(memberKind, memberAccess);

        // --- modifier legality checks (conservative per JLS)
        validateModifiers(memberKind, memberAccess, declarationModifiers);

        this.memberKind = memberKind;
        this.memberAccess = memberAccess; // validated above for presence/absence
        this.declarationModifiers = unmodifiableSet(declarationModifiers);
        this.annotationQualifiedNames = unmodifiableSet(annotationQualifiedNames);
    }

    private static @Nullable String validateAndNormalizeName(@Nullable String rawName, @NonNull MemberKind memberKind) {
        // Normalize once
        String trimmedName = trimToNull(rawName);

        // For initializers the name MUST be null; for all others it MUST be non-null.
        boolean mustBeNull = memberKind == MemberKind.INIT_BLOCK_STATIC || memberKind == MemberKind.INIT_BLOCK_INSTANCE;
        boolean isProvided = trimmedName != null;

        if (mustBeNull == isProvided) {
            // Both true => initializer has a name (illegal)
            // Both false => non-initializer without a name (illegal)
            throw new IllegalArgumentException(
                    mustBeNull
                            ? "Initializer elements must have null name"
                            : "Non-initializer elements must have a non-blank name");
        }

        // Valid: return null for initializers, normalized name for others.
        return mustBeNull ? null : trimmedName;
    }

    private static void validateAccessForMemberKind(
            @NonNull MemberKind memberKind, @Nullable MemberAccess memberAccess) {
        boolean allows = memberKind.isAccessLevelApplicable();
        boolean provided = (memberAccess != null);

        if (allows != provided) {
            String message = allows
                    ? "Access level must be provided for " + memberKind
                    : "Access level must be null for " + memberKind;
            throw new IllegalArgumentException(message);
        }
    }

    private static void validateModifiers(
            @NonNull MemberKind memberKind,
            @Nullable MemberAccess memberAccess,
            @NonNull Set<@NonNull DeclarationModifier> declarationModifiers) {

        ensureNoModifiersIfRequired(memberKind, declarationModifiers);

        if (memberKind == MemberKind.FIELD) {
            validateFieldModifiers(memberKind, declarationModifiers);
            return;
        }

        if (memberKind == MemberKind.METHOD) {
            // memberAccess is guaranteed non-null for METHOD by validateAccessForMemberKind
            validateMethodModifiers(memberKind, memberAccess, declarationModifiers);
            return;
        }

        if (memberKind.isType()) {
            validateTypeModifiers(memberKind, declarationModifiers);
        }
    }

    private static void ensureNoModifiersIfRequired(
            @NonNull MemberKind memberKind, @NonNull Set<@NonNull DeclarationModifier> declarationModifiers) {

        if (KINDS_WITHOUT_MODIFIERS.contains(memberKind) && !declarationModifiers.isEmpty()) {
            throw new IllegalArgumentException(memberKind + " must not declare modifiers: " + declarationModifiers);
        }
    }

    private static void validateFieldModifiers(
            @NonNull MemberKind memberKind, @NonNull Set<@NonNull DeclarationModifier> declarationModifiers) {
        forbidAny(
                memberKind,
                declarationModifiers,
                DeclarationModifier.ABSTRACT,
                DeclarationModifier.SYNCHRONIZED,
                DeclarationModifier.NATIVE,
                DeclarationModifier.DEFAULT,
                DeclarationModifier.SEALED,
                DeclarationModifier.NON_SEALED,
                DeclarationModifier.STRICTFP // not applicable to fields
                );
    }

    private static void validateMethodModifiers(
            @NonNull MemberKind memberKind,
            @Nullable MemberAccess memberAccess,
            @NonNull Set<@NonNull DeclarationModifier> declarationModifiers) {
        // Forbid field/type-only modifiers
        forbidAny(
                memberKind,
                declarationModifiers,
                DeclarationModifier.TRANSIENT,
                DeclarationModifier.VOLATILE,
                DeclarationModifier.SEALED,
                DeclarationModifier.NON_SEALED);

        // Conflicts with ABSTRACT
        if (declarationModifiers.contains(DeclarationModifier.ABSTRACT)) {
            forbidAny(
                    memberKind,
                    declarationModifiers,
                    DeclarationModifier.FINAL,
                    DeclarationModifier.STATIC,
                    DeclarationModifier.NATIVE,
                    DeclarationModifier.SYNCHRONIZED);
            if (memberAccess == MemberAccess.PRIVATE) {
                throw new IllegalArgumentException("Illegal modifier/access for METHOD: abstract + private");
            }
        }

        // Default (interface) method conflicts
        if (declarationModifiers.contains(DeclarationModifier.DEFAULT)) {
            forbidAny(
                    memberKind,
                    declarationModifiers,
                    DeclarationModifier.ABSTRACT,
                    DeclarationModifier.STATIC,
                    DeclarationModifier.FINAL,
                    DeclarationModifier.SYNCHRONIZED,
                    DeclarationModifier.NATIVE);
        }
    }

    private static void validateTypeModifiers(
            @NonNull MemberKind memberKind, @NonNull Set<@NonNull DeclarationModifier> declarationModifiers) {
        // Forbid method/field-only modifiers
        forbidAny(
                memberKind,
                declarationModifiers,
                DeclarationModifier.TRANSIENT,
                DeclarationModifier.VOLATILE,
                DeclarationModifier.SYNCHRONIZED,
                DeclarationModifier.NATIVE,
                DeclarationModifier.DEFAULT);

        // Conflicts for types
        requireNotBoth(memberKind, declarationModifiers, DeclarationModifier.ABSTRACT, DeclarationModifier.FINAL);
        requireNotBoth(memberKind, declarationModifiers, DeclarationModifier.SEALED, DeclarationModifier.NON_SEALED);
    }

    private static void forbidAny(
            MemberKind memberKind, Set<DeclarationModifier> modifiers, DeclarationModifier... forbiddenModifiers) {
        Arrays.stream(forbiddenModifiers).filter(modifiers::contains).findAny().ifPresent(m -> {
            throw new IllegalArgumentException("Illegal modifier for " + memberKind + ": " + m);
        });
    }

    private static void requireNotBoth(
            MemberKind memberKind,
            Set<DeclarationModifier> modifiers,
            DeclarationModifier first,
            DeclarationModifier second) {
        if (modifiers.contains(first) && modifiers.contains(second)) {
            throw new IllegalArgumentException(
                    "Illegal modifier combination for " + memberKind + ": " + first + " + " + second);
        }
    }

    /**
     * True if this element is a (nested) type declaration.
     */
    public boolean isType() {
        return memberKind.isType();
    }

    /**
     * True if this element is an initializer block.
     */
    public boolean isInitializer() {
        return memberKind == MemberKind.INIT_BLOCK_STATIC || memberKind == MemberKind.INIT_BLOCK_INSTANCE;
    }

    /**
     * Optional-returning getter for name.
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Optional-returning getter for access level.
     */
    public Optional<MemberAccess> getMemberAccess() {
        return Optional.ofNullable(memberAccess);
    }

    // --- equals / hashCode (hand-written, lean for SpotBugs) ------------------
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EffectiveMemberDescriptor that)) {
            return false;
        }
        return this.memberKind == that.memberKind
                && Objects.equals(this.memberAccess, that.memberAccess)
                && this.declarationModifiers.equals(that.declarationModifiers)
                && this.annotationQualifiedNames.equals(that.annotationQualifiedNames)
                && Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        int result = memberKind.hashCode();
        result = 31 * result + Objects.hashCode(memberAccess);
        result = 31 * result + declarationModifiers.hashCode();
        result = 31 * result + annotationQualifiedNames.hashCode();
        result = 31 * result + Objects.hashCode(name);
        return result;
    }

    /**
     * Unifies members and nested types; each constant knows whether it represents a type and whether access applies.
     */
    @Getter
    @RequiredArgsConstructor
    public enum MemberKind {
        FIELD(false, true),
        METHOD(false, true),
        CONSTRUCTOR(false, true),
        INIT_BLOCK_STATIC(false, false),
        INIT_BLOCK_INSTANCE(false, false),
        ENUM_CONSTANT(false, false),
        RECORD_COMPONENT(false, false),

        TYPE_CLASS(true, true),
        TYPE_INTERFACE(true, true),
        TYPE_ENUM(true, true),
        TYPE_RECORD(true, true),
        TYPE_ANNOTATION(true, true),
        ;

        private final boolean type;
        // Whether an explicit access modifier is applicable/required for this kind.
        private final boolean accessLevelApplicable;
    }

    public enum MemberAccess {
        PUBLIC,
        PROTECTED,
        PACKAGE,
        PRIVATE,
    }

    /**
     * Unified declaration modifiers used in rules; extend as needed.
     */
    public enum DeclarationModifier {
        STATIC,
        FINAL,
        ABSTRACT,
        DEFAULT, // interface method only
        SYNCHRONIZED,
        TRANSIENT, // fields only
        VOLATILE, // fields only
        NATIVE, // methods only
        STRICTFP, // types and methods
        SEALED, // types (Java 17)
        NON_SEALED, // types (Java 17)
    }
}
