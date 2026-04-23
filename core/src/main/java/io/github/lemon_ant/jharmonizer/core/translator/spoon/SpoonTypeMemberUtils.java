// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.GROUP_HEADER_METADATA;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Internal utilities for inspecting Spoon type members during source printing.
 * Provides helpers for resolving effective source ranges, group headers, and leading comments.
 */
@UtilityClass
class SpoonTypeMemberUtils {

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
