// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import spoon.reflect.declaration.CtTypeMember;

/** Assigns and resolves group separators attached to Spoon members. */
@UtilityClass
public class SpoonGroupSeparatorUtils {

    @NonNull
    private static final GroupSeparator BLANK_LINE_SEPARATOR = new GroupSeparator(null, UnifiedSeparator.NEW_LINE);

    @NonNull
    private static final String GROUP_HEADER_METADATA = "GROUP_HEADER";

    @NonNull
    private static final String GROUP_SEPARATOR_NEW_LINE = "\n";

    @NonNull
    private static final GroupSeparator NO_SEPARATOR = new GroupSeparator(null, UnifiedSeparator.NONE);

    /**
     * Assigns a blank-line separator to a member.
     * @param member member starting the group
     */
    public static void markBlankLine(@NonNull CtTypeMember member) {
        markSeparator(member, GROUP_SEPARATOR_NEW_LINE);
    }

    /**
     * Assigns header text without trimming or escaping it.
     * @param member member starting the group
     * @param headerText header content
     */
    public static void markHeader(@NonNull CtTypeMember member, @NonNull String headerText) {
        markSeparator(member, headerText);
    }

    /**
     * Resolves the assigned separator without normalizing header text.
     * @param member member to inspect
     * @return separator kind and optional header text; {@code NONE} when unassigned
     */
    @NonNull
    public static GroupSeparator resolveSeparator(@NonNull CtTypeMember member) {
        Object separatorMetadata = member.getMetadata(GROUP_HEADER_METADATA);
        String separatorText = separatorMetadata == null ? null : separatorMetadata.toString();
        if (separatorText == null) {
            return NO_SEPARATOR;
        }
        return GROUP_SEPARATOR_NEW_LINE.equals(separatorText)
                ? BLANK_LINE_SEPARATOR
                : new GroupSeparator(separatorText, UnifiedSeparator.HEADER);
    }

    private static void markSeparator(CtTypeMember member, String separatorText) {
        member.putMetadata(GROUP_HEADER_METADATA, separatorText);
    }

    /** Resolved separator; header text is present only for {@code HEADER}, including empty headers. */
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class GroupSeparator {

        @Nullable
        String headerText;

        @NonNull
        UnifiedSeparator type;
    }
}
