// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.jspecify.annotations.Nullable;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;

/** Spoon type operations shared by parsing, sorting, and source printing. */
@UtilityClass
public class SpoonTypeUtils {
    private static final int ONE_ROOT_TYPE = 1;

    /**
     * Finds the earliest start reported by Spoon for the member and its positioned annotations.
     * In the annotation group gap fixture, Spoon reports the start of {@code CommentedAnnotation} at a line comment
     * after {@code @Ccc} and {@code @Bbb}. Taking the minimum includes those annotations and retains any earlier comments
     * already within the reported member range.
     * @param member source-positioned type member to inspect
     * @return first source offset including annotations outside the member's reported source range
     */
    // TODO(SPOON_SOURCE_START): Recheck CommentedAnnotation in printer scenario 21-annotation-group-gaps
    // after the next Spoon GA upgrade. Reproduced on 11.5.0 (latest GA checked 2026-10-19).
    // Remove this correction only when reported starts include earlier annotations and retain leading comments.
    public static int findEffectiveMemberStart(@NonNull CtTypeMember member) {
        // Spoon 11.5.0 reports CommentedAnnotation's start at "// moves with Bbb", after both @Ccc and @Bbb.
        // Taking the minimum restores both annotations and retains earlier comments already in the member range.
        return member.getAnnotations().stream()
                .map(CtAnnotation::getPosition)
                .filter(SourcePosition::isValidPosition)
                .mapToInt(SourcePosition::getSourceStart)
                .reduce(member.getPosition().getSourceStart(), Math::min);
    }

    /**
     * Finds the main type.
     * @param compilationUnit Spoon model of the source file to inspect
     * @return the matching main type
     */
    @Nullable
    public static CtType<?> findMainType(@NonNull CtCompilationUnit compilationUnit) {
        List<CtType<?>> declaredTypes = compilationUnit.getDeclaredTypes();
        if (declaredTypes.size() == ONE_ROOT_TYPE) {
            return declaredTypes.get(0);
        }

        String baseName =
                FilenameUtils.removeExtension(compilationUnit.getFile().getName());
        CtType<?> fileNameMatchType = null;
        for (CtType<?> type : declaredTypes) {
            if (type.hasModifier(ModifierKind.PUBLIC)) {
                // Any java file can contain only one main public class, that is main
                return type;
            }

            // If there is no public class, we fall back to the class that matches the file name by its class name
            if (fileNameMatchType == null && type.getSimpleName().equals(baseName)) {
                fileNameMatchType = type;
            }
        }

        // If name matching was found, then return this matched class, otherwise null
        return fileNameMatchType;
    }

    /**
     * Streams explicit, source-positioned members in their current declaration-list order.
     * @param declaringType source type whose direct members are inspected
     * @return explicit source members, excluding implicit declarations unsupported by the source printer
     */
    @NonNull
    public static Stream<@NonNull CtTypeMember> streamExplicitSrcTypeMembers(@NonNull CtType<?> declaringType) {
        return declaringType.getTypeMembers().stream()
                .filter(typeMember -> typeMember.getPosition() != null
                        && typeMember.getPosition().isValidPosition())
                /* TODO(RECORDS_DISABLED): Remove this guard to start processing record implicit fields/components.
                Disabled until the source printer can correctly print record headers/components. */
                .filter(typeMember -> !typeMember.isImplicit());
    }
}
