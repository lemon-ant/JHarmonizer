// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtCompilationUnit;

@UtilityClass
public class SpoonAstModelTestCreator {

    /**
     * Copies a parsed fixture with a cloned compilation unit and shared type declarations.
     * @param templateModel parsed fixture to copy
     * @return model with the copied compilation unit and matching source data and type references
     */
    @NonNull
    public static SpoonAstModel copyWithClonedCompilationUnit(@NonNull SpoonAstModel templateModel) {
        // Spoon 11.5.0 clones type references in the same factory, so they still resolve to the original declarations.
        // Reuse their opt-outs and member snapshot; independent benchmark models need separate factory isolation.
        CtCompilationUnit compilationUnit = templateModel.getCompilationUnit().clone();
        return SpoonAstModel.builder()
                .annotationSrcGroups(templateModel.getAnnotationSrcGroups())
                .compilationUnit(compilationUnit)
                .mainType(templateModel.getMainType().orElse(null))
                .optOuts(templateModel.getOptOuts())
                .originalMemberOrder(templateModel.getOriginalMemberOrder())
                .path(templateModel.getPath())
                .srcCode(templateModel.getSrcCode())
                .build();
    }
}
