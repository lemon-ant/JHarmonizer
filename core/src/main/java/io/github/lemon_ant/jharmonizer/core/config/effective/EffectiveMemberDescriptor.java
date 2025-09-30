package io.github.lemon_ant.jharmonizer.core.config.effective;

import static java.util.Collections.unmodifiableSet;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
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
 * Unifies members and nested types via MemberKind and TargetCategory; access level and declaration modifiers
 * are captured explicitly to drive selectors and ordering logic.
 */
@Value
@Builder
public class EffectiveMemberDescriptor {

    public static final int ONE = 1;
    /**
     * Simple name. Must be null for INIT_BLOCK and CONSTRUCTOR; must be non-blank for all other categories.
     */
    @Nullable
    String name;

    /**
     * Unified kind: fields, methods, constructors, init blocks, enum constants, record components, and nested types.
     */
    @NonNull
    MemberKind memberKind;

    /**
     * Access level (PACKAGE means no explicit modifier).
     * For kinds whose category does not support access (see TargetCategory#isAccessLevelApplicable()), this must be null.
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

        // --- modifier legality checks (via TargetCategory + conflicts)
        validateModifiers(memberKind, memberAccess, declarationModifiers);

        this.memberKind = memberKind;
        this.memberAccess = memberAccess; // validated above for presence/absence
        this.declarationModifiers = unmodifiableSet(declarationModifiers);
        this.annotationQualifiedNames = unmodifiableSet(annotationQualifiedNames);
    }

    private static @Nullable String validateAndNormalizeName(@Nullable String rawName, @NonNull MemberKind memberKind) {
        String trimmedName = trimToNull(rawName);

        // Only INIT_BLOCK and CONSTRUCTOR must have null name; all other categories must provide a non-blank name.
        TargetCategory category = memberKind.getTargetCategory();
        boolean mustBeNull = (category == TargetCategory.INIT_BLOCK) || (category == TargetCategory.CONSTRUCTOR);
        boolean isProvided = trimmedName != null;

        if (mustBeNull == isProvided) {
            throw new IllegalArgumentException(
                    mustBeNull
                            ? "Initializer/constructor elements must have null name"
                            : "Non-initializer elements must have a non-blank name");
        }
        return trimmedName;
    }

    private static void validateAccessForMemberKind(
            @NonNull MemberKind memberKind, @Nullable MemberAccess memberAccess) {

        boolean applicable = memberKind.getTargetCategory().isAccessLevelApplicable();
        boolean provided = (memberAccess != null);

        if (applicable != provided) {
            String message = applicable
                    ? "Access level must be provided for " + memberKind
                    : "Access level must be null for " + memberKind;
            throw new IllegalArgumentException(message);
        }
    }

    private static void validateModifiers(
            @NonNull MemberKind memberKind,
            @Nullable MemberAccess memberAccess,
            @NonNull Set<@NonNull DeclarationModifier> declarationModifiers) {

        final TargetCategory targetCategory = memberKind.getTargetCategory();

        validateModifierApplicability(memberKind, targetCategory, declarationModifiers);
        validateModifierPairwiseConflicts(memberKind, declarationModifiers);
        enforceAbstractNotPrivate(targetCategory, memberAccess, declarationModifiers);
    }

    private static void validateModifierApplicability(
            @NonNull MemberKind memberKind,
            @NonNull TargetCategory targetCategory,
            @NonNull Set<@NonNull DeclarationModifier> declarationModifiers) {

        declarationModifiers.stream()
                .filter(declarationModifier -> !declarationModifier.isApplicableTo(targetCategory))
                .findAny()
                .ifPresent(illegalModifier -> {
                    throw new IllegalArgumentException("Illegal modifier for " + memberKind + ": " + illegalModifier);
                });
    }

    private static void validateModifierPairwiseConflicts(
            @NonNull MemberKind memberKind, @NonNull Set<@NonNull DeclarationModifier> declarationModifiers) {

        if (declarationModifiers.size() > ONE) {
            final DeclarationModifier[] declarationModifierArray =
                    declarationModifiers.toArray(new DeclarationModifier[0]);
            for (int leftIndex = 0; leftIndex < declarationModifierArray.length - ONE; leftIndex++) {
                final DeclarationModifier leftModifier = declarationModifierArray[leftIndex];
                for (int rightIndex = leftIndex + ONE; rightIndex < declarationModifierArray.length; rightIndex++) {
                    final DeclarationModifier rightModifier = declarationModifierArray[rightIndex];
                    if (leftModifier.conflictsWithOn(rightModifier)) {
                        throw new IllegalArgumentException("Illegal modifier combination for " + memberKind + ": "
                                + leftModifier + " + " + rightModifier);
                    }
                }
            }
        }
    }

    private static void enforceAbstractNotPrivate(
            @NonNull TargetCategory targetCategory,
            @Nullable MemberAccess memberAccess,
            @NonNull Set<@NonNull DeclarationModifier> declarationModifiers) {

        if (targetCategory == TargetCategory.METHOD
                && declarationModifiers.contains(DeclarationModifier.ABSTRACT)
                && memberAccess == MemberAccess.PRIVATE) {
            throw new IllegalArgumentException("Illegal modifier/access for METHOD: abstract + private");
        }
    }

    /**
     * True if this element is a (nested) type declaration.
     */
    public boolean isType() {
        return memberKind.isType();
    }

    /**
     * True if this element is an initializer block (static or instance).
     */
    public boolean isInitializer() {
        return memberKind.isInitializer();
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
     * Coarse-grained target category that drives applicability of access and declaration modifiers.
     */
    @Getter
    @RequiredArgsConstructor
    public enum TargetCategory {
        FIELD(true),
        METHOD(true),
        CONSTRUCTOR(true),
        INIT_BLOCK(false), // static / instance initializer blocks
        ENUM_CONSTANT(false), // enum constant entries
        RECORD_COMPONENT(false), // record component entries
        TYPE(true), // all TYPE_* kinds
        ;

        /**
         * Whether explicit access level applies (must be provided).
         */
        private final boolean accessLevelApplicable;

        /** True if this category represents a (nested) type. */
        public boolean isType() {
            return this == TYPE;
        }

        public boolean isInitializer() {
            return this == TargetCategory.INIT_BLOCK;
        }
    }

    /**
     * Unifies members and nested types; each constant binds to a TargetCategory.
     */
    @Getter
    @RequiredArgsConstructor
    public enum MemberKind {
        FIELD(TargetCategory.FIELD),
        METHOD(TargetCategory.METHOD),
        CONSTRUCTOR(TargetCategory.CONSTRUCTOR),

        // Initializer blocks:
        INIT_BLOCK_STATIC(TargetCategory.INIT_BLOCK),
        INIT_BLOCK_INSTANCE(TargetCategory.INIT_BLOCK),

        // Non-block entries that must have names (distinct from init blocks):
        ENUM_CONSTANT(TargetCategory.ENUM_CONSTANT),
        RECORD_COMPONENT(TargetCategory.RECORD_COMPONENT),

        // Types:
        TYPE_CLASS(TargetCategory.TYPE),
        TYPE_INTERFACE(TargetCategory.TYPE),
        TYPE_ENUM(TargetCategory.TYPE),
        TYPE_RECORD(TargetCategory.TYPE),
        TYPE_ANNOTATION(TargetCategory.TYPE),
        ;

        private final TargetCategory targetCategory;

        public boolean isType() {
            return this.getTargetCategory().isType();
        }

        public boolean isInitializer() {
            return this.getTargetCategory().isInitializer();
        }
    }

    public enum MemberAccess {
        PUBLIC,
        PROTECTED,
        PACKAGE,
        PRIVATE,
    }

    /**
     * Unified declaration modifiers used in rules; applicability is defined via TargetCategory sets.
     * Pairwise conflicts are declared globally (category-agnostic) and checked in {@link #validateModifiers}.
     */
    public enum DeclarationModifier {
        STATIC(EnumSet.of(TargetCategory.FIELD, TargetCategory.METHOD, TargetCategory.TYPE)),
        FINAL(EnumSet.of(TargetCategory.FIELD, TargetCategory.METHOD, TargetCategory.TYPE)),
        ABSTRACT(EnumSet.of(TargetCategory.METHOD, TargetCategory.TYPE)),
        DEFAULT(EnumSet.of(TargetCategory.METHOD)), // interface default methods
        SYNCHRONIZED(EnumSet.of(TargetCategory.METHOD)),
        TRANSIENT(EnumSet.of(TargetCategory.FIELD)),
        VOLATILE(EnumSet.of(TargetCategory.FIELD)),
        NATIVE(EnumSet.of(TargetCategory.METHOD)),
        STRICTFP(EnumSet.of(TargetCategory.METHOD, TargetCategory.TYPE)),
        SEALED(EnumSet.of(TargetCategory.TYPE)),
        NON_SEALED(EnumSet.of(TargetCategory.TYPE)),
        ;

        @Getter
        private final Set<TargetCategory> applicableTargets;

        // Global symmetric conflicts (no TargetCategory scoping)
        @SuppressWarnings("PMD.UseEnumCollections")
        private final Set<DeclarationModifier> conflicts = new HashSet<>();

        private DeclarationModifier(Set<TargetCategory> applicableTargets) {
            this.applicableTargets = Collections.unmodifiableSet(applicableTargets);
        }

        public boolean isApplicableTo(TargetCategory targetCategory) {
            return applicableTargets.contains(targetCategory);
        }

        /**
         * Global conflict check. Category is ignored intentionally:
         * applicability per category is validated separately before conflicts are checked.
         */
        public boolean conflictsWithOn(@NonNull DeclarationModifier other) {
            return this.conflicts.contains(other);
        }

        // -- static conflict registry helpers (symmetric, category-agnostic) --

        private static void conflict(DeclarationModifier a, DeclarationModifier b) {
            a.conflicts.add(b);
            b.conflicts.add(a);
        }

        static {
            // METHOD-level pairs (but safe globally because of applicability filtering):
            conflict(ABSTRACT, FINAL);
            conflict(ABSTRACT, STATIC);
            conflict(ABSTRACT, NATIVE);
            conflict(ABSTRACT, SYNCHRONIZED);

            conflict(DEFAULT, ABSTRACT);
            conflict(DEFAULT, STATIC);
            conflict(DEFAULT, FINAL);
            conflict(DEFAULT, SYNCHRONIZED);
            conflict(DEFAULT, NATIVE);

            // TYPE-level pair (SEALED vs NON_SEALED) — applies only on types via applicability:
            conflict(SEALED, NON_SEALED);

            // No FIELD-only conflicts in the current minimal model.
        }
    }
}
