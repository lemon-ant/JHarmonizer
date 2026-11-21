// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtTypeMember;

/** Exposes parsed-model hierarchy snapshots to tests in other processing packages. */
@UtilityClass
public class SpoonTypeTreeTestUtils {

    /**
     * Collects top-level types and explicit source members in their current model hierarchy order.
     * @param compilationUnit Spoon model of the source file to inspect
     * @return immutable list of types and members, recursively descending into member types
     */
    @NonNull
    public static List<CtTypeMember> collectDeclaredHierarchy(@NonNull CtCompilationUnit compilationUnit) {
        return SpoonTypeTreeUtils.streamDeclaredHierarchy(compilationUnit).toList();
    }
}
