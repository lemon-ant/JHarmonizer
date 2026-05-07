// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.optout;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import java.util.Comparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import spoon.reflect.code.CtComment;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.visitor.filter.TypeFilter;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class JHarmonizerOptOutFileScopeResolver {

    @NonNull
    private final CtCompilationUnit compilationUnit;

    @NonNull
    private final SrcFile srcFile;

    /**
     * Resolves file-scope opt-out mode with AST-first logic and raw-source fallback.
     *
     * @param srcFile source file associated with the parsed compilation unit
     * @param compilationUnit parsed compilation unit
     * @return resolved file-scope opt-out mode, or {@code null} when no directive is present
     */
    @Nullable
    static JHarmonizerOptOutMode resolveFileOptOutMode(
            @NonNull SrcFile srcFile, @NonNull CtCompilationUnit compilationUnit) {
        return new JHarmonizerOptOutFileScopeResolver(compilationUnit, srcFile).resolveFileOptOutMode();
    }

    private static boolean isBeforeFirstDeclaredType(CtComment comment, CtCompilationUnit compilationUnit) {
        return comment.getPosition().getSourceEnd()
                < compilationUnit.getDeclaredTypes().stream()
                        .map(CtElement::getPosition)
                        .mapToInt(SourcePosition::getSourceStart)
                        .min()
                        .orElse(Integer.MAX_VALUE);
    }

    @Nullable
    private static JHarmonizerOptOutMode resolveFromCandidates(List<FileScopeOptOutCandidate> optOutCandidates) {
        JHarmonizerOptOutMode fileOptOutMode = null;
        FileScopeOptOutCandidate previousCandidate = null;
        for (FileScopeOptOutCandidate optOutCandidate : optOutCandidates) {
            JHarmonizerOptOutMode currentMode = optOutCandidate.getOptOutMode();
            if (currentMode == null) {
                continue;
            }
            if (fileOptOutMode != null) {
                JHarmonizerOptOutCommentUtilities.logIgnoredFileOptOutAtLocation(
                        optOutCandidate.getLocation(),
                        "Later file-scope opt-out replaces the previously parsed one from %s; the last applicable"
                                        .formatted(previousCandidate.getLocation())
                                + " file-scope opt-out wins");
            }

            fileOptOutMode = currentMode;
            previousCandidate = optOutCandidate;
            if (fileOptOutMode == JHarmonizerOptOutMode.FULLY_OFF) {
                break;
            }
        }
        return fileOptOutMode;
    }

    private boolean requiresRawSrcFallback() {
        CtCompilationUnit.UNIT_TYPE unitType = compilationUnit.getUnitType();
        if (unitType == CtCompilationUnit.UNIT_TYPE.PACKAGE_DECLARATION
                || unitType == CtCompilationUnit.UNIT_TYPE.MODULE_DECLARATION) {
            return true;
        }
        return unitType == CtCompilationUnit.UNIT_TYPE.TYPE_DECLARATION
                && compilationUnit.getDeclaredTypes().isEmpty();
    }

    @Nullable
    private JHarmonizerOptOutMode resolveFileOptOutMode() {
        if (requiresRawSrcFallback()) {
            return resolveFromRawSrc();
        }
        return resolveFromAstComments();
    }

    @Nullable
    private JHarmonizerOptOutMode resolveFromAstComments() {
        List<FileScopeOptOutCandidate> fileComments =
                compilationUnit.getElements(new TypeFilter<>(CtComment.class)).stream()
                        .filter(comment -> isBeforeFirstDeclaredType(comment, compilationUnit))
                        .sorted(Comparator.comparingInt(
                                comment -> comment.getPosition().getSourceStart()))
                        .map(comment -> new FileScopeOptOutCandidate(
                                JHarmonizerOptOutCommentUtilities.formatLocation(srcFile, comment.getPosition()),
                                JHarmonizerOptOutCommentUtilities.parseTypeOptOutMode(comment)))
                        .toList();
        return resolveFromCandidates(fileComments);
    }

    @Nullable
    private JHarmonizerOptOutMode resolveFromRawSrc() {
        List<FileScopeOptOutCandidate> rawFileComments =
                JHarmonizerOptOutCommentUtilities.collectRawCommentsByRegex(srcFile.getSrcCode()).stream()
                        .map(rawCommentMatch -> new FileScopeOptOutCandidate(
                                JHarmonizerOptOutCommentUtilities.formatLocation(
                                        srcFile, rawCommentMatch.getCommentOffset()),
                                JHarmonizerOptOutCommentUtilities.parseFileScopeOptOutMode(
                                        rawCommentMatch.getRawComment(), rawCommentMatch.getCommentOffset(), srcFile)))
                        .toList();
        return resolveFromCandidates(rawFileComments);
    }

    @Value
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class FileScopeOptOutCandidate {

        @NonNull
        String location;

        @Nullable
        JHarmonizerOptOutMode optOutMode;
    }
}
