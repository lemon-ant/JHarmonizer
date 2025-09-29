package io.github.lemon_ant.jharmonizer.core.config.effective;

import static io.github.lemon_ant.jharmonizer.core.config.effective.EffectiveMemberDescriptor.DeclarationModifier.NON_SEALED;
import static java.util.Collections.unmodifiableSet;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Arrays;
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
public class EffectiveMemberDescriptor {

    /** Simple name (null for initializer blocks). */
    @Nullable
    String name;

    /** Unified kind: fields, methods, ctors, init blocks, enum consts, record components, and nested types. */
    @NonNull
    MemberKind memberKind;

    /** Access level (PACKAGE means no explicit modifier). */
    @NonNull
    MemberAccess memberAccess;

    /** Unified set of declaration modifiers (STATIC, FINAL, ABSTRACT, DEFAULT, SEALED, NON_SEALED, etc.). */
    @NonNull
    @Singular
    Set<@NonNull DeclarationModifier> declarationModifiers;

    /** Fully-qualified annotation names. */
    @NonNull
    @Singular
    Set<@NonNull String> annotationQualifiedNames;

    /** True if this element is a (nested) type declaration. */
    public boolean isType() {
        return memberKind.isType();
    }

    /** True if this element is an initializer block. */
    public boolean isInitializer() {
        return memberKind == MemberKind.INIT_BLOCK_STATIC || memberKind == MemberKind.INIT_BLOCK_INSTANCE;
    }

    /** Optional-returning getter for name. */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    private EffectiveMemberDescriptor(
            @Nullable String name,
            @NonNull EffectiveMemberDescriptor.MemberKind memberKind,
            @NonNull EffectiveMemberDescriptor.MemberAccess memberAccess,
            @NonNull Set<@NonNull DeclarationModifier> declarationModifiers,
            @NonNull Set<@NonNull String> annotationQualifiedNames) {

        String trimmedName = trimToNull(name);
        // Validate element name rules:
        if (memberKind == MemberKind.INIT_BLOCK_STATIC || memberKind == MemberKind.INIT_BLOCK_INSTANCE) {
            // Initializers must NOT have a name
            if (trimmedName != null) {
                throw new IllegalArgumentException("Initializer elements must have null name");
            }
        } else if (trimmedName == null) {
            // All non-initializers must have a non-blank name
            throw new IllegalArgumentException("Non-initializer elements must have a non-blank name");
        }
        this.name = trimmedName;

        // Modifier legality checks (conservative per JLS).
        validateModifiersForMemberKind(memberKind, memberAccess, declarationModifiers);

        this.memberKind = memberKind;
        this.memberAccess = memberAccess;
        this.declarationModifiers = unmodifiableSet(declarationModifiers);
        this.annotationQualifiedNames = unmodifiableSet(annotationQualifiedNames);
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private static void validateModifiersForMemberKind(
            @NonNull MemberKind memberKind,
            @NonNull MemberAccess memberAccess,
            @NonNull Set<@NonNull DeclarationModifier> declarationModifiers) {

        // Kinds that must not declare any modifiers:
        switch (memberKind) {
            case CONSTRUCTOR:
            case INIT_BLOCK_STATIC:
            case INIT_BLOCK_INSTANCE:
            case ENUM_CONSTANT:
            case RECORD_COMPONENT:
                if (!declarationModifiers.isEmpty()) {
                    throw new IllegalArgumentException(
                            memberKind + " must not declare modifiers: " + declarationModifiers);
                }
                return;
            default:
                // fall through
        }

        // Fields: forbid method/type-only modifiers.
        if (memberKind == MemberKind.FIELD) {
            forbidAny(
                    memberKind,
                    declarationModifiers,
                    DeclarationModifier.ABSTRACT,
                    DeclarationModifier.SYNCHRONIZED,
                    DeclarationModifier.NATIVE,
                    DeclarationModifier.DEFAULT,
                    DeclarationModifier.SEALED,
                    NON_SEALED,
                    DeclarationModifier.STRICTFP // not applicable to fields
                    );
            return;
        }

        // Methods (non-constructors).
        if (memberKind == MemberKind.METHOD) {
            // Forbid field/type-only modifiers.
            forbidAny(
                    memberKind,
                    declarationModifiers,
                    DeclarationModifier.TRANSIENT,
                    DeclarationModifier.VOLATILE,
                    DeclarationModifier.SEALED,
                    NON_SEALED);

            // Conflicts with ABSTRACT.
            if (declarationModifiers.contains(DeclarationModifier.ABSTRACT)) {
                forbidAny(
                        memberKind,
                        declarationModifiers,
                        DeclarationModifier.FINAL,
                        DeclarationModifier.STATIC,
                        DeclarationModifier.NATIVE,
                        DeclarationModifier.SYNCHRONIZED);
                // Abstract method cannot be private.
                if (memberAccess == MemberAccess.PRIVATE) {
                    throw new IllegalArgumentException("Illegal modifier/access for METHOD: abstract + private");
                }
            }

            // Default (interface) method cannot be abstract/static/final/synchronized/native.
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
            return;
        }

        // Nested types: forbid method/field-only modifiers and check conflicts.
        if (memberKind.isType()) {
            forbidAny(
                    memberKind,
                    declarationModifiers,
                    DeclarationModifier.TRANSIENT,
                    DeclarationModifier.VOLATILE,
                    DeclarationModifier.SYNCHRONIZED,
                    DeclarationModifier.NATIVE,
                    DeclarationModifier.DEFAULT);

            // Conflicts for types: ABSTRACT + FINAL.
            requireNotBoth(memberKind, declarationModifiers, DeclarationModifier.ABSTRACT, DeclarationModifier.FINAL);

            // Mutually exclusive: SEALED vs NON_SEALED.
            requireNotBoth(
                    memberKind, declarationModifiers, DeclarationModifier.SEALED, DeclarationModifier.NON_SEALED);
        }
    }

    private static void forbidAny(
            MemberKind memberKind, Set<DeclarationModifier> mods, DeclarationModifier... forbiddenModifiers) {
        Arrays.stream(forbiddenModifiers).filter(mods::contains).findAny().ifPresent(m -> {
            throw new IllegalArgumentException("Illegal modifier for " + memberKind + ": " + m);
        });
    }

    private static void requireNotBoth(
            MemberKind memberKind,
            Set<DeclarationModifier> declarationModifiers,
            DeclarationModifier declarationModifier1,
            DeclarationModifier declarationModifier2) {
        if (declarationModifiers.contains(declarationModifier1)
                && declarationModifiers.contains(declarationModifier2)) {
            throw new IllegalArgumentException("Illegal modifier combination for " + memberKind + ": "
                    + declarationModifier1 + " + " + declarationModifier2);
        }
    }

    // --- equals / hashCode (hand-written, lean for SpotBugs) ------------------
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EffectiveMemberDescriptor that)) {
            return false;
        }
        return this.memberKind == that.memberKind
                && this.memberAccess == that.memberAccess
                && this.declarationModifiers.equals(that.declarationModifiers)
                && this.annotationQualifiedNames.equals(that.annotationQualifiedNames)
                && Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        int result = memberKind.hashCode();
        result = 31 * result + memberAccess.hashCode();
        result = 31 * result + declarationModifiers.hashCode();
        result = 31 * result + annotationQualifiedNames.hashCode();
        result = 31 * result + Objects.hashCode(name);
        return result;
    }

    /** Unifies members and nested types; each constant knows whether it represents a type. */
    @Getter
    @RequiredArgsConstructor
    public enum MemberKind {
        FIELD(false),
        METHOD(false),
        CONSTRUCTOR(false),
        INIT_BLOCK_STATIC(false),
        INIT_BLOCK_INSTANCE(false),
        ENUM_CONSTANT(false),
        RECORD_COMPONENT(false),

        TYPE_CLASS(true),
        TYPE_INTERFACE(true),
        TYPE_ENUM(true),
        TYPE_RECORD(true),
        TYPE_ANNOTATION(true),
        ;

        private final boolean type;
    }

    public enum MemberAccess {
        PUBLIC,
        PROTECTED,
        PACKAGE,
        PRIVATE,
    }

    /** Unified declaration modifiers used in rules; extend as needed. */
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
