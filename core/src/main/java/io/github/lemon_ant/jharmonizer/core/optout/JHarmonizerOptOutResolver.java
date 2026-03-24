package io.github.lemon_ant.jharmonizer.core.optout;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import spoon.reflect.code.CtComment;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class JHarmonizerOptOutResolver {

    @NonNull
    private final CtCompilationUnit compilationUnit;

    @NonNull
    private final SrcFile srcFile;

    /**
     * Resolves file-level and type-level opt-out directives from a parsed compilation unit.
     *
     * @param srcFile the source file that owns the parsed compilation unit
     * @param compilationUnit the parsed compilation unit to inspect
     * @return the resolved opt-out summary
     */
    @NonNull
    public static JHarmonizerOptOuts resolve(@NonNull SrcFile srcFile, @NonNull CtCompilationUnit compilationUnit) {
        return new JHarmonizerOptOutResolver(compilationUnit, srcFile).resolve();
    }

    @NonNull
    private JHarmonizerOptOuts resolve() {
        JHarmonizerOptOutMode fileOptOutMode =
                JHarmonizerOptOutFileScopeResolver.resolveFileOptOutMode(srcFile, compilationUnit);
        if (fileOptOutMode == JHarmonizerOptOutMode.FULLY_OFF) {
            return new JHarmonizerOptOuts(JHarmonizerOptOutMode.FULLY_OFF, Map.of());
        }

        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtType<?>, JHarmonizerOptOutMode> typeOptOutModes = new HashMap<>();
        boolean sortingDisabledInParents = fileOptOutMode == JHarmonizerOptOutMode.SORTING_OFF;
        for (CtType<?> declaredType : compilationUnit.getDeclaredTypes()) {
            collectTypeOptOutModes(declaredType, sortingDisabledInParents, typeOptOutModes);
        }

        return typeOptOutModes.isEmpty() && fileOptOutMode == null
                ? JHarmonizerOptOuts.empty()
                : new JHarmonizerOptOuts(fileOptOutMode, typeOptOutModes);
    }

    private void collectTypeOptOutModes(
            CtType<?> currentType,
            boolean sortingDisabledInParents,
            Map<CtType<?>, JHarmonizerOptOutMode> typeOptOutModes) {
        JHarmonizerOptOutMode directOptOutMode = findTypeOptOutMode(currentType);
        if (directOptOutMode == JHarmonizerOptOutMode.FULLY_OFF) {
            typeOptOutModes.put(currentType, JHarmonizerOptOutMode.FULLY_OFF);
            return;
        }

        boolean sortingDisabledForCurrentType = sortingDisabledInParents;
        if (directOptOutMode == JHarmonizerOptOutMode.SORTING_OFF && !sortingDisabledInParents) {
            typeOptOutModes.put(currentType, JHarmonizerOptOutMode.SORTING_OFF);
            sortingDisabledForCurrentType = true;
        }

        for (CtType<?> nestedType : currentType.getNestedTypes()) {
            collectTypeOptOutModes(nestedType, sortingDisabledForCurrentType, typeOptOutModes);
        }
    }

    @Nullable
    private JHarmonizerOptOutMode findTypeOptOutMode(CtType<?> currentType) {
        JHarmonizerOptOutMode typeOptOutMode = null;
        CtComment previousTypeComment = null;
        for (CtComment leadingTypeComment : collectLeadingTypeComments(currentType)) {
            JHarmonizerOptOutMode currentMode = JHarmonizerOptOutCommentSupport.parseTypeOptOutMode(leadingTypeComment);
            if (currentMode == null) {
                continue;
            }
            if (typeOptOutMode != null) {
                JHarmonizerOptOutCommentSupport.logIgnoredTypeOptOut(
                        leadingTypeComment,
                        "Later opt-out comment for type '%s' replaces the previously parsed one from %s; the last"
                                        .formatted(
                                                currentType.getQualifiedName(),
                                                JHarmonizerOptOutCommentSupport.formatLocation(
                                                        srcFile, previousTypeComment.getPosition()))
                                + " applicable type-level opt-out wins",
                        srcFile);
            }

            typeOptOutMode = currentMode;
            previousTypeComment = leadingTypeComment;
            if (typeOptOutMode == JHarmonizerOptOutMode.FULLY_OFF) {
                break;
            }
        }
        return typeOptOutMode;
    }

    @NonNull
    private static java.util.List<CtComment> collectLeadingTypeComments(CtType<?> currentType) {
        return currentType.getComments().stream()
                .filter(comment -> comment.getPosition().getEndLine()
                        < currentType.getPosition().getLine())
                .sorted(Comparator.comparingInt(comment -> comment.getPosition().getSourceStart()))
                .toList();
    }
}
