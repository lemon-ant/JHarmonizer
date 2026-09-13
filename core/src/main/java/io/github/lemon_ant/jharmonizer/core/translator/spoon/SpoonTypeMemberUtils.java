// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/** Inspects explicit declarations, source boundaries and attached comments. */
@UtilityClass
class SpoonTypeMemberUtils {

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
     * Detects leading comments, excluding misplaced trailing comments and enclosing-type JavaDoc.
     * @param member the member to inspect
     * @param memberDeclarationEndLines the set of last source lines of declarations in the same type
     * @param typeDeclarationStartLine the enclosing type's declaration line
     * @return {@code true} if the member has a genuine leading comment
     */
    static boolean hasLeadingCommentOnSeparateLine(
            @NonNull CtTypeMember member,
            @NonNull Set<Integer> memberDeclarationEndLines,
            int typeDeclarationStartLine) {
        return member.getComments().stream()
                .filter(comment -> comment.getPosition().isValidPosition())
                // Spoon can attach a sibling's trailing comment or the enclosing type's JavaDoc to this member.
                .filter(comment -> !memberDeclarationEndLines.contains(
                        comment.getPosition().getLine()))
                .filter(comment -> comment.getPosition().getLine() >= typeDeclarationStartLine)
                .anyMatch(comment -> comment.getPosition().getEndLine()
                        < member.getPosition().getLine());
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
}
