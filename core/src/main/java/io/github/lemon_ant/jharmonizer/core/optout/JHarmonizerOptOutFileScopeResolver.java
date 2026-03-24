package io.github.lemon_ant.jharmonizer.core.optout;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import java.util.Comparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import spoon.reflect.code.CtComment;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.visitor.filter.TypeFilter;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class JHarmonizerOptOutFileScopeResolver {

    @NonNull
    private final SrcFile srcFile;

    @NonNull
    private final CtCompilationUnit compilationUnit;

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
        return new JHarmonizerOptOutFileScopeResolver(srcFile, compilationUnit).resolveFileOptOutMode();
    }

    @Nullable
    private JHarmonizerOptOutMode resolveFileOptOutMode() {
        if (requiresRawSourceFallback()) {
            return resolveFromRawSource();
        }
        return resolveFromAstComments();
    }

    private boolean requiresRawSourceFallback() {
        CtCompilationUnit.UNIT_TYPE unitType = compilationUnit.getUnitType();
        if (unitType == CtCompilationUnit.UNIT_TYPE.PACKAGE_DECLARATION
                || unitType == CtCompilationUnit.UNIT_TYPE.MODULE_DECLARATION) {
            return true;
        }
        return unitType == CtCompilationUnit.UNIT_TYPE.TYPE_DECLARATION
                && compilationUnit.getDeclaredTypes().isEmpty();
    }

    @Nullable
    private JHarmonizerOptOutMode resolveFromAstComments() {
        List<FileScopeOptOutCandidate> fileComments =
                compilationUnit.getElements(new TypeFilter<>(CtComment.class)).stream()
                        .filter(comment -> isBeforeFirstDeclaredType(comment, compilationUnit))
                        .sorted(Comparator.comparingInt(
                                comment -> comment.getPosition().getSourceStart()))
                        .map(comment -> new FileScopeOptOutCandidate(
                                JHarmonizerOptOutCommentUtilities.parseTypeOptOutMode(comment),
                                JHarmonizerOptOutCommentUtilities.formatLocation(srcFile, comment.getPosition())))
                        .toList();
        return resolveFromCandidates(fileComments);
    }

    @Nullable
    private JHarmonizerOptOutMode resolveFromRawSource() {
        List<FileScopeOptOutCandidate> rawFileComments =
                JHarmonizerOptOutCommentUtilities.collectRawCommentsByRegex(srcFile.getSrcCode()).stream()
                        .map(rawCommentMatch -> new FileScopeOptOutCandidate(
                                JHarmonizerOptOutCommentUtilities.parseFileScopeOptOutMode(
                                        rawCommentMatch.getRawComment(), rawCommentMatch.getCommentOffset(), srcFile),
                                JHarmonizerOptOutCommentUtilities.formatLocation(
                                        srcFile, rawCommentMatch.getCommentOffset())))
                        .toList();
        return resolveFromCandidates(rawFileComments);
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

    private static boolean isBeforeFirstDeclaredType(CtComment comment, CtCompilationUnit compilationUnit) {
        return comment.getPosition().getSourceEnd()
                < compilationUnit.getDeclaredTypes().stream()
                        .map(CtElement::getPosition)
                        .mapToInt(SourcePosition::getSourceStart)
                        .min()
                        .orElse(Integer.MAX_VALUE);
    }

    @Value
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class FileScopeOptOutCandidate {
        @Nullable
        JHarmonizerOptOutMode optOutMode;

        @NonNull
        String location;
    }
}
