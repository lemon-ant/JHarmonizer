package io.github.lemon_ant.jharmonizer.core.config.unified;

import static java.util.Collections.unmodifiableSet;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.config.compiled.MemberDeclarationFlagsUtil;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

/**
 * Java AST parser-agnostic descriptor for selection and sorting rules.
 * Unifies members and nested types via MemberKind and TargetCategory; access level and declaration modifiers
 * are captured explicitly to drive selectors and ordering logic.
 */
@Value
public class MemberDescriptor {

    private static final int ONE = 1;
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
    Set<@NonNull DeclarationModifier> declarationModifiers;

    /**
     * Fully-qualified annotation names.
     */
    @NonNull
    Set<@NonNull String> annotationQualifiedNames;

    int featureMask;

    @Builder
    private MemberDescriptor(
            @Nullable String name,
            @NonNull MemberKind memberKind,
            @Nullable MemberAccess memberAccess,
            @NonNull @Singular Set<@NonNull DeclarationModifier> declarationModifiers,
            @NonNull @Singular Set<@NonNull String> annotationQualifiedNames) {

        this.name = validateAndNormalizeName(name, memberKind);

        // --- access invariants
        validateAccessForMemberKind(memberKind, memberAccess);

        // --- modifier legality checks (via TargetCategory + conflicts)
        validateModifiers(memberKind, memberAccess, declarationModifiers);

        this.memberKind = memberKind;
        this.memberAccess = memberAccess; // validated above for presence/absence
        this.declarationModifiers = unmodifiableSet(declarationModifiers);
        this.annotationQualifiedNames = unmodifiableSet(annotationQualifiedNames);

        // Предвычисляем featureMask один раз (kind + access + modifiers)
        this.featureMask = MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(
                this.memberKind, this.memberAccess, this.declarationModifiers);
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
                    if (leftModifier.hasConflictWith(rightModifier)) {
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
        if (!(other instanceof MemberDescriptor that)) {
            return false;
        }
        // featureMask покрывает: memberKind + memberAccess + declarationModifiers
        return this.featureMask == that.featureMask
                && Objects.equals(this.name, that.name)
                && this.annotationQualifiedNames.equals(that.annotationQualifiedNames);
    }

    @Override
    public int hashCode() {
        int result = featureMask;
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + annotationQualifiedNames.hashCode();
        return result;
    }
}
