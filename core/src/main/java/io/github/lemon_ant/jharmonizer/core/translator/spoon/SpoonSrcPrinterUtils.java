package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Internal utilities shared between the custom Spoon source printer and its callers.
 * Provides helpers for detecting the dominant line separator, compiled predicate factories
 * for blank-line decisions based on {@link PrinterConfig}, and type-member inspection helpers.
 */
@UtilityClass
public class SpoonSrcPrinterUtils {

    public static final String GROUP_HEADER_METADATA = "GROUP_HEADER";
    public static final String GROUP_SEPARATOR_NEW_LINE = "\n";

    /**
     * Detects the dominant line separator.
     *
     * @param source the source code text to inspect
     * @return the dominant line separator
     */
    @NonNull
    @SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.AvoidReassigningLoopVariables"})
    static String detectDominantLineSeparator(@NonNull String src) {
        if (src.isEmpty()) {
            return System.lineSeparator();
        }

        int crlfCount = 0;
        int lfCount = 0;
        int crCount = 0;

        for (int index = 0; index < src.length(); index++) {
            char currentChar = src.charAt(index);

            if (currentChar == '\r') {
                boolean hasNextChar = (index + 1) < src.length();
                if (hasNextChar && src.charAt(index + 1) == '\n') {
                    crlfCount++;
                    index++; // skip '\n' in CRLF
                } else {
                    crCount++; // classic Mac style: CR only
                }
                continue;
            }

            if (currentChar == '\n') {
                lfCount++; // Unix/macOS modern style: LF only
            }
        }

        return selectDominantLineSeparator(crlfCount, lfCount, crCount);
    }

    @NonNull
    private static String selectDominantLineSeparator(int crlfCount, int lfCount, int crCount) {
        if (crlfCount == 0 && lfCount == 0 && crCount == 0) {
            return System.lineSeparator();
        }

        // Pick the dominant one; if tie, prefer CRLF > LF > CR (can be adjusted).
        if (crlfCount >= lfCount && crlfCount >= crCount) {
            return "\r\n";
        }
        if (lfCount >= crCount) {
            return "\n";
        }
        return "\r";
    }

    /**
     * Compiles a predicate that determines whether a separator is needed after a given member.
     * Annotation-based blank lines are always active (Palantir formatter enforces them).
     * When {@code blankLineBetweenFields} is enabled, all fields get a separator after them.
     *
     * @param config the printer configuration
     * @return a predicate that returns {@code true} when a separator is needed after the member
     */
    @NonNull
    static Predicate<CtTypeMember> compileNeedsSeparatorAfter(@NonNull PrinterConfig config) {
        if (config.isBlankLineBetweenFields()) {
            return member -> true;
        }
        Predicate<CtTypeMember> isNotField = member -> !(member instanceof CtField);
        return isNotField.or(member -> !member.getAnnotations().isEmpty());
    }

    /**
     * Compiles a bi-predicate that determines whether a separator is needed before a given member.
     * Annotation-based blank lines are always active (Palantir formatter enforces them).
     * The blank-line-before-comment feature is handled separately in the printer via
     * {@link #hasLeadingCommentOnSeparateLine}, because it requires per-type context
     * (the set of member source lines) to filter out Spoon's misattributed trailing inline comments.
     *
     * @return a bi-predicate accepting (member, isFirst) that returns {@code true} when a separator is needed
     */
    @NonNull
    static BiPredicate<CtTypeMember, Boolean> compileNeedsSeparatorBefore() {
        BiPredicate<CtTypeMember, Boolean> basePredicate = compileBaseSeparatorBeforePredicate();
        BiPredicate<CtTypeMember, Boolean> annotationCheck =
                (member, first) -> !member.getAnnotations().isEmpty();
        return annotationCheck.or(basePredicate);
    }

    /**
     * Returns {@code true} when the member has at least one genuine leading comment: a comment
     * whose end line is strictly before the member's own line, and whose start line does not
     * coincide with the last source line of any other member declaration.
     *
     * <p>The second filter guards against Spoon's comment misattribution after member reordering.
     * When members are reordered, Spoon sometimes attributes a trailing {@code //} comment from
     * one member to the next element in the original source order. Such a comment always sits on
     * the last line of the member it was originally trailing (its {@code endLine}), so filtering
     * by {@code memberDeclarationEndLines} removes these spurious attributions while leaving
     * genuine leading comments (which occupy their own lines, not the end line of a declaration)
     * intact.
     *
     * @param member the member to inspect
     * @param memberDeclarationEndLines the set of last source lines of declarations in the same type
     * @return {@code true} if the member has a genuine leading comment
     */
    static boolean hasLeadingCommentOnSeparateLine(
            @NonNull CtTypeMember member, @NonNull Set<Integer> memberDeclarationEndLines) {
        return member.getComments().stream()
                .filter(comment -> comment.getPosition().isValidPosition())
                .filter(comment -> !memberDeclarationEndLines.contains(
                        comment.getPosition().getLine()))
                .anyMatch(comment -> comment.getPosition().getEndLine()
                        < member.getPosition().getLine());
    }

    /**
     * Compiles a predicate that determines whether a blank line should be inserted after
     * the type declaration header, before the first member. The predicate is compiled once.
     * Enums always get a blank line after the header (after the constant list, before methods).
     * When the flag is enabled, all types get a blank line after the header.
     *
     * @param config the printer configuration
     * @return a predicate accepting the type and returning {@code true} when a blank line is needed
     */
    @NonNull
    static Predicate<CtType<?>> compileNeedsBlankLineAfterTypeHeader(@NonNull PrinterConfig config) {
        if (config.isBlankLineAfterTypeHeader()) {
            return type -> true;
        }
        return type -> type instanceof CtEnum<?>;
    }

    @NonNull
    private static BiPredicate<CtTypeMember, Boolean> compileBaseSeparatorBeforePredicate() {
        return (member, first) -> {
            boolean isNotField = !(member instanceof CtField);
            if (!first && isNotField) {
                return true;
            }

            Optional<String> groupHeaderMetadata = Optional.ofNullable(member.getMetadata(GROUP_HEADER_METADATA))
                    .map(Object::toString);
            return groupHeaderMetadata
                    .map(groupHeader -> !GROUP_SEPARATOR_NEW_LINE.equals(groupHeader))
                    .orElse(false);
        };
    }

    /**
     * Returns the explicit (source-positioned, non-implicit) type members of the given type.
     *
     * @param type the type declaration to inspect
     * @return the list of explicit type members
     */
    @NonNull
    static List<CtTypeMember> findExplicitTypeMembers(@NonNull CtType<?> type) {
        return type.getTypeMembers().stream()
                // Spoon creates implicit constructors which don't exist in the source code
                .filter(typeMember -> typeMember.getPosition().isValidPosition())
                /* TODO(RECORDS_DISABLED): Remove this guard when record headers/components are printed correctly.
                Today implicit record fields/components still produce wrong source-printer output. */
                .filter(typeMember -> !typeMember.isImplicit())
                .toList();
    }

    /**
     * Returns the source end of the last trailing comment attached by Spoon to this member,
     * or the member's own source end when no such comment exists.
     * This prevents trailing comments from being cut off when there is no next member.
     *
     * @param member the type member to inspect
     * @return the inclusive source index of the effective end of this member
     */
    static int findEffectiveMemberEnd(@NonNull CtTypeMember member) {
        int memberEnd = member.getPosition().getSourceEnd();
        return member.getComments().stream()
                .filter(comment -> comment.getPosition().isValidPosition())
                .filter(comment -> comment.getPosition().getSourceStart() > memberEnd)
                .mapToInt(comment -> comment.getPosition().getSourceEnd())
                .max()
                .orElse(memberEnd);
    }

    /**
     * Returns whether the member has a leading comment whose content matches the given group header.
     *
     * @param member      the type member to inspect
     * @param groupHeader the expected group header text (trimmed, without comment delimiters)
     * @return {@code true} if a matching leading comment exists
     */
    static boolean hasMatchingLeadingComment(@NonNull CtTypeMember member, @NonNull String groupHeader) {
        return member.getComments().stream()
                .filter(comment -> comment.getPosition().getEndLine()
                        < member.getPosition().getLine())
                .map(comment -> comment.getContent().trim())
                .anyMatch(groupHeader::equals);
    }

    /**
     * Returns the group-header metadata string attached to the member, or {@code null} if absent.
     *
     * @param member the type member to inspect
     * @return the group header, or {@code null}
     */
    @Nullable
    static String findGroupHeader(@NonNull CtTypeMember member) {
        Object groupHeaderMetadata = member.getMetadata(GROUP_HEADER_METADATA);
        if (groupHeaderMetadata == null) {
            return null;
        }
        return groupHeaderMetadata.toString();
    }
}
