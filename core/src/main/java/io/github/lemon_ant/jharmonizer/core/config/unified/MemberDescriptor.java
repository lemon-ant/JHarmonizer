package io.github.lemon_ant.jharmonizer.core.config.unified;

import static java.util.Collections.unmodifiableSet;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

/**
 * Java AST translator-agnostic descriptor for selection and sorting rules.
 * Unifies members and nested types via MemberKind and TargetCategory; access level and declaration modifiers
 * are captured explicitly to drive selectors and ordering logic.
 */
@Value
public class MemberDescriptor {
    private static final int ONE = 1;

    /**
     * Fully-qualified annotation names.
     */
    @NonNull
    Set<@NonNull String> annotationQualifiedNames;

    /**
     * Unified set of declaration modifiers (STATIC, FINAL, ABSTRACT, DEFAULT, SEALED, NON_SEALED, etc.).
     */
    @NonNull
    Set<@NonNull DeclarationModifier> declarationModifiers;

    int featureMask;

    /**
     * Access level (PACKAGE means no explicit modifier).
     * For kinds whose category does not support access (see TargetCategory#isAccessLevelApplicable()), this must be null.
     */
    @Nullable
    MemberAccess memberAccess;

    /**
     * Unified kind: fields, methods, constructors, init blocks, enum constants, record components, and nested types.
     */
    @NonNull
    MemberKind memberKind;

    // TODO ParametersDescriptor to sort based on it
    /**
     * Simple name. Must be null for INIT_BLOCK and CONSTRUCTOR; must be non-blank for all other categories.
     */
    @Nullable
    String name;

    // TODO Remove builder
    @Builder
    private MemberDescriptor(
            @Nullable String name,
            @NonNull MemberKind memberKind,
            @Nullable MemberAccess memberAccess,
            @NonNull @Singular Set<@NonNull DeclarationModifier> declarationModifiers,
            @NonNull @Singular Set<@NonNull String> annotationQualifiedNames) {

        this.name = validateAndNormalizeName(
                name, memberKind, memberAccess, declarationModifiers, annotationQualifiedNames);

        String validationContext = formatValidationContext(
                name, this.name, memberKind, memberAccess, declarationModifiers, annotationQualifiedNames);

        // --- access invariants
        validateAccessForMemberKind(memberKind, memberAccess, validationContext);

        // --- modifier legality checks (via TargetCategory + conflicts)
        validateModifiers(memberKind, memberAccess, declarationModifiers, validationContext);

        this.memberKind = memberKind;
        this.memberAccess = memberAccess; // validated above for presence/absence
        this.declarationModifiers = unmodifiableSet(new TreeSet<>(declarationModifiers));
        this.annotationQualifiedNames = unmodifiableSet(annotationQualifiedNames);

        // Precompute featureMask once (kind + access + modifiers)
        this.featureMask = MemberDeclarationFlagsUtil.encodeMemberDeclarationFlags(
                this.memberKind, this.memberAccess, this.declarationModifiers);
    }

    private static void enforceAbstractNotPrivate(
            TargetCategory targetCategory,
            @Nullable MemberAccess memberAccess,
            Set<DeclarationModifier> declarationModifiers,
            String validationContext) {

        if (targetCategory == TargetCategory.METHOD
                && declarationModifiers.contains(DeclarationModifier.ABSTRACT)
                && memberAccess == MemberAccess.PRIVATE) {
            throw new IllegalArgumentException(
                    "Illegal modifier/access combination for METHOD: abstract + private. " + validationContext);
        }
    }

    private static void enforceAbstractNotStaticMethod(
            TargetCategory targetCategory, Set<DeclarationModifier> declarationModifiers) {

        if (targetCategory == TargetCategory.METHOD
                && declarationModifiers.contains(DeclarationModifier.ABSTRACT)
                && declarationModifiers.contains(DeclarationModifier.STATIC)) {
            throw new IllegalArgumentException("Illegal modifier combination for METHOD: abstract + static");
        }
    }

    private static void validateAccessForMemberKind(
            MemberKind memberKind, @Nullable MemberAccess memberAccess, String validationContext) {

        boolean accessLevelApplicable = memberKind.getTargetCategory().isAccessLevelApplicable();
        boolean accessLevelProvided = (memberAccess != null);

        if (accessLevelApplicable != accessLevelProvided) {
            String message = accessLevelApplicable
                    ? "Access level must be provided for " + memberKind
                    : "Access level must be null for " + memberKind + ". " + validationContext;
            throw new IllegalArgumentException(message);
        }
    }

    @Nullable
    private static String validateAndNormalizeName(
            @Nullable String rawName,
            MemberKind memberKind,
            @Nullable MemberAccess memberAccess,
            Set<DeclarationModifier> declarationModifiers,
            Set<String> annotationQualifiedNames) {

        String trimmedName = trimToNull(rawName);

        // Only INIT_BLOCK and CONSTRUCTOR must have null name; all other categories must provide a non-blank name.
        TargetCategory targetCategory = memberKind.getTargetCategory();
        boolean nameMustBeNull =
                (targetCategory == TargetCategory.INIT_BLOCK) || (targetCategory == TargetCategory.CONSTRUCTOR);
        boolean nameProvided = trimmedName != null;

        if (nameMustBeNull == nameProvided) {
            // TODO Take it as a method parameter
            String validationContext = formatValidationContext(
                    rawName, trimmedName, memberKind, memberAccess, declarationModifiers, annotationQualifiedNames);

            String message = nameMustBeNull
                    ? "Name invariant violated: initializer/constructor elements must have null name."
                    : "Name invariant violated: non-initializer elements must have a non-blank name.";

            throw new IllegalArgumentException(message + " " + validationContext);
        }

        return trimmedName;
    }

    private static void validateModifierApplicability(
            MemberKind memberKind,
            TargetCategory targetCategory,
            Set<DeclarationModifier> declarationModifiers,
            String validationContext) {
        declarationModifiers.stream()
                .filter(declarationModifier -> !declarationModifier.isApplicableTo(targetCategory))
                .findAny()
                .ifPresent(illegalModifier -> {
                    throw new IllegalArgumentException("Illegal modifier for memberKind=" + memberKind
                            + ", targetCategory=" + targetCategory
                            + ", illegalModifier=" + illegalModifier
                            + ". " + validationContext);
                });
    }

    private static void validateModifierPairwiseConflicts(
            MemberKind memberKind, Set<DeclarationModifier> declarationModifiers, String validationContext) {

        if (declarationModifiers.size() > ONE) {
            DeclarationModifier[] declarationModifierArray = declarationModifiers.toArray(new DeclarationModifier[0]);
            for (int leftIndex = 0; leftIndex < declarationModifierArray.length - ONE; leftIndex++) {
                DeclarationModifier leftModifier = declarationModifierArray[leftIndex];
                for (int rightIndex = leftIndex + ONE; rightIndex < declarationModifierArray.length; rightIndex++) {
                    DeclarationModifier rightModifier = declarationModifierArray[rightIndex];
                    if (leftModifier.hasConflictWith(rightModifier)) {
                        throw new IllegalArgumentException(
                                "Illegal modifier combination for memberKind=" + memberKind + ": "
                                        + leftModifier + " + " + rightModifier
                                        + ". " + validationContext);
                    }
                }
            }
        }
    }

    private static void validateModifiers(
            MemberKind memberKind,
            @Nullable MemberAccess memberAccess,
            Set<DeclarationModifier> declarationModifiers,
            String validationContext) {

        TargetCategory targetCategory = memberKind.getTargetCategory();

        validateModifierApplicability(memberKind, targetCategory, declarationModifiers, validationContext);
        validateModifierPairwiseConflicts(memberKind, declarationModifiers, validationContext);
        enforceAbstractNotPrivate(targetCategory, memberAccess, declarationModifiers, validationContext);
        enforceAbstractNotStaticMethod(targetCategory, declarationModifiers);
    }

    @NonNull
    private static String formatValidationContext(
            @Nullable String rawName,
            @Nullable String normalizedName,
            MemberKind memberKind,
            @Nullable MemberAccess memberAccess,
            Set<DeclarationModifier> declarationModifiers,
            Set<String> annotationQualifiedNames) {

        return "\ncontext{memberKind=" + memberKind
                + ",\ntargetCategory=" + memberKind.getTargetCategory()
                + ",\nmemberAccess=" + memberAccess
                + ",\nname.raw=" + rawName
                + ",\nname.normalized=" + normalizedName
                + ",\ndeclarationModifiers=" + declarationModifiers
                + ",\nannotationQualifiedNames=" + annotationQualifiedNames
                + "}";
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
        // featureMask covers: memberKind + memberAccess + declarationModifiers
        return this.featureMask == that.featureMask
                && Objects.equals(this.name, that.name)
                && this.annotationQualifiedNames.equals(that.annotationQualifiedNames);
    }

    /**
     * Optional-returning getter for access level.
     */
    @NonNull
    public Optional<MemberAccess> getMemberAccess() {
        return Optional.ofNullable(memberAccess);
    }

    /**
     * Optional-returning getter for name.
     */
    @NonNull
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    @Override
    public int hashCode() {
        int result = featureMask;
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + annotationQualifiedNames.hashCode();
        return result;
    }

    /**
     * True if this element is an initializer block (static or instance).
     */
    public boolean isInitializer() {
        return memberKind.isInitializer();
    }

    /**
     * True if this element is a (nested) type declaration.
     */
    public boolean isType() {
        return memberKind.isType();
    }
}
