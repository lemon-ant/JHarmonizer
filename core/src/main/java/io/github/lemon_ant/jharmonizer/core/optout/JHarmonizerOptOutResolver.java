package io.github.lemon_ant.jharmonizer.core.optout;

import io.github.lemon_ant.jharmonizer.core.common.SrcFile;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtComment.CommentType;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;

@Slf4j
@RequiredArgsConstructor
public final class JHarmonizerOptOutResolver {
    @NonNull
    private final SrcFile srcFile;

    @NonNull
    private final CtCompilationUnit compilationUnit;

    @NonNull
    private final JHarmonizerOptOutPlacementResolver placementResolver;

    @NonNull
    public static JHarmonizerOptOuts resolve(@NonNull SrcFile srcFile, @NonNull CtCompilationUnit compilationUnit) {
        return new JHarmonizerOptOutResolver(
                        srcFile, compilationUnit, new JHarmonizerOptOutPlacementResolver(compilationUnit))
                .resolve();
    }

    @NonNull
    private JHarmonizerOptOuts resolve() {
        List<CtComment> potentialOptOutComments =
                JHarmonizerOptOutCommentCollector.findPotentialOptOutComments(compilationUnit);
        if (potentialOptOutComments.isEmpty()) {
            return JHarmonizerOptOuts.empty();
        }

        ConcurrentMap<SourcePosition, ResolvedJHarmonizerOptOut> typeOptOuts = new ConcurrentHashMap<>();
        ResolvedJHarmonizerOptOut fileOptOut = null;
        for (CtComment potentialOptOutComment : potentialOptOutComments) {
            Optional<JHarmonizerOptOutMode> optOutMode = parseOptOutMode(potentialOptOutComment);
            if (optOutMode.isEmpty()) {
                continue;
            }

            Optional<ResolvedJHarmonizerOptOut> typeOptOut =
                    tryResolveTypeOptOut(potentialOptOutComment, optOutMode.orElseThrow());
            if (typeOptOut.isPresent()) {
                storeTypeOptOut(typeOptOuts, typeOptOut.orElseThrow(), potentialOptOutComment);
                continue;
            }

            if (placementResolver.isMemberLevelOptOut(potentialOptOutComment)) {
                logIgnoredOptOut(potentialOptOutComment, "Member-level JHarmonizer opt-out comments are ignored");
                continue;
            }

            if (placementResolver.isFileScopeOptOut(potentialOptOutComment)) {
                fileOptOut = storeFileOptOut(fileOptOut, potentialOptOutComment, optOutMode.orElseThrow());
                continue;
            }

            logIgnoredOptOut(potentialOptOutComment, "Opt-out comment is not in a supported file or type location");
        }

        return JHarmonizerOptOuts.of(fileOptOut, typeOptOuts);
    }

    @NonNull
    private Optional<JHarmonizerOptOutMode> parseOptOutMode(@NonNull CtComment comment) {
        if (comment.getCommentType() == CommentType.JAVADOC) {
            logIgnoredOptOut(comment, "Javadoc opt-out comments are ignored");
            return Optional.empty();
        }

        String normalizedContent = comment.getContent().trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalizedContent.startsWith(JHarmonizerOptOutMode.TOKEN_PREFIX)) {
            logIgnoredOptOut(comment, "Malformed opt-out comment is ignored");
            return Optional.empty();
        }

        try {
            return Optional.of(JHarmonizerOptOutMode.fromToken(normalizedContent));
        } catch (IllegalArgumentException exception) {
            logIgnoredOptOut(comment, exception.getMessage());
            return Optional.empty();
        }
    }

    @NonNull
    private Optional<ResolvedJHarmonizerOptOut> tryResolveTypeOptOut(
            @NonNull CtComment comment, @NonNull JHarmonizerOptOutMode mode) {
        return placementResolver
                .findTypeTarget(comment)
                .map(targetType -> resolveTypeOptOut(comment, mode, targetType));
    }

    private void storeTypeOptOut(
            @NonNull Map<SourcePosition, ResolvedJHarmonizerOptOut> typeOptOuts,
            @NonNull ResolvedJHarmonizerOptOut resolvedOptOut,
            @NonNull CtComment comment) {
        ResolvedJHarmonizerOptOut previousOptOut = typeOptOuts.putIfAbsent(
                resolvedOptOut.getTargetType().orElseThrow().getPosition(), resolvedOptOut);
        if (previousOptOut != null) {
            logIgnoredOptOut(
                    comment,
                    "Duplicate opt-out for type '%s'; keeping the first one from %s"
                            .formatted(
                                    resolvedOptOut.getTargetType().orElseThrow().getQualifiedName(),
                                    formatLocation(previousOptOut.getCommentPosition())));
        }
    }

    @NonNull
    private ResolvedJHarmonizerOptOut storeFileOptOut(
            ResolvedJHarmonizerOptOut currentFileOptOut,
            @NonNull CtComment comment,
            @NonNull JHarmonizerOptOutMode mode) {
        ResolvedJHarmonizerOptOut resolvedOptOut = new ResolvedJHarmonizerOptOut(
                comment.getPosition(), mode, null, JHarmonizerOptOutScope.FILE_SCOPE, null);
        if (currentFileOptOut != null) {
            logIgnoredOptOut(
                    comment,
                    "Conflicting file-scope opt-out; keeping the first one from %s"
                            .formatted(formatLocation(currentFileOptOut.getCommentPosition())));
            return currentFileOptOut;
        }
        return resolvedOptOut;
    }

    @NonNull
    private ResolvedJHarmonizerOptOut resolveTypeOptOut(
            @NonNull CtComment comment, @NonNull JHarmonizerOptOutMode mode, @NonNull CtType<?> targetType) {
        SourceCharacterRange preservedSourceRange = new SourceCharacterRange(
                findIndentationStart(srcFile.getSrcCode(), comment.getPosition().getSourceStart()),
                targetType.getPosition().getSourceEnd() + 1);
        return new ResolvedJHarmonizerOptOut(
                comment.getPosition(), mode, preservedSourceRange, JHarmonizerOptOutScope.TYPE_SCOPE, targetType);
    }

    private static int findIndentationStart(@NonNull String sourceCode, int startIndex) {
        int index = startIndex - 1;
        while (index >= 0) {
            char symbol = sourceCode.charAt(index);
            if (symbol != ' ' && symbol != '\t') {
                break;
            }
            index--;
        }
        return index + 1;
    }

    private void logIgnoredOptOut(@NonNull CtComment comment, @NonNull String message) {
        if (log.isWarnEnabled()) {
            log.warn("{} at {}", message, formatLocation(comment.getPosition()));
        }
    }

    @NonNull
    private String formatLocation(@NonNull SourcePosition sourcePosition) {
        return srcFile.getPath() + ":" + sourcePosition.getLine() + ":" + sourcePosition.getColumn();
    }
}
