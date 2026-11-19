// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOuts;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/** Parsed source context and shared working AST; selected print orders belong to {@link SpoonPrintingInput}. */
@Value
@Builder(access = PACKAGE)
@AllArgsConstructor(access = PRIVATE)
public class SpoonAstModel {

    /**
     * Shared working AST; one sorting attempt consumes it across all model views.
     * Sorting reorders declarations but preserves annotation lists.
     */
    @NonNull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    CtCompilationUnit compilationUnit;

    /** Main type resolved during parsing, absent for package/module sources. */
    @Nullable
    CtType<?> mainType;

    /** Parsed opt-out directives used to preserve excluded types during sorting and printing. */
    @NonNull
    JHarmonizerOptOuts optOuts;

    /** Original lexer-derived annotation groups, shared unchanged by sorting and printing. */
    @NonNull
    List<AnnotationSrcGroup> originalAnnotationSrcGroups;

    /** Original member traversal order captured by the parser for relocation detection. */
    @NonNull
    List<CtTypeMember> originalMemberOrder;

    /** Input path used in diagnostics, including virtual sources. */
    @NonNull
    Path path;

    /** Exact input text; annotation and Spoon source offsets refer to this string. */
    @NonNull
    String srcCode;

    /**
     * Returns the main type.
     * @return the main type
     */
    @NonNull
    public Optional<CtType<?>> getMainType() {
        return Optional.ofNullable(mainType);
    }
}
