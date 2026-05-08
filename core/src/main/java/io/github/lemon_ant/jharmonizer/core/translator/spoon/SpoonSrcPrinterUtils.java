// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
/*
 * SPDX-FileCopyrightText: Contributors to the jharmonizer project
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import java.util.Optional;
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
 * Provides helpers for detecting the dominant line separator and compiled predicate factories
 * for blank-line decisions based on {@link PrinterConfig}.
 * Type-member and comment inspection helpers live in {@link SpoonTypeMemberUtils}.
 */
@UtilityClass
public class SpoonSrcPrinterUtils {
    public static final String GROUP_HEADER_METADATA = "GROUP_HEADER";
    public static final String GROUP_SEPARATOR_NEW_LINE = "\n";

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
     * {@link SpoonTypeMemberUtils#hasLeadingCommentOnSeparateLine}, because it requires per-type context
     * (the set of member declaration end lines) to filter out Spoon's misattributed trailing inline comments.
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
}
