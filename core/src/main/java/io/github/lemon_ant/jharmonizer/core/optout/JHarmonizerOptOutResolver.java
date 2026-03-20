package io.github.lemon_ant.jharmonizer.core.optout;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtComment.CommentType;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.visitor.filter.TypeFilter;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class JHarmonizerOptOutResolver {
    @NonNull
    private final SrcFile srcFile;

    @NonNull
    private final CtCompilationUnit compilationUnit;

    @NonNull
    public static JHarmonizerOptOuts resolve(@NonNull SrcFile srcFile, @NonNull CtCompilationUnit compilationUnit) {
        return new JHarmonizerOptOutResolver(srcFile, compilationUnit).resolve();
    }

    @NonNull
    private JHarmonizerOptOuts resolve() {
        JHarmonizerOptOutMode fileOptOutMode = resolveFileOptOutMode();
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

    @Nullable
    private JHarmonizerOptOutMode resolveFileOptOutMode() {
        List<CtComment> fileComments = compilationUnit.getElements(new TypeFilter<>(CtComment.class)).stream()
                .filter(JHarmonizerOptOutResolver::isStandaloneComment)
                .filter(this::isBeforeFirstDeclaredType)
                .sorted(Comparator.comparingInt(comment -> comment.getPosition().getSourceStart()))
                .toList();
        JHarmonizerOptOutMode fileOptOutMode = null;
        CtComment firstFileComment = null;
        for (CtComment fileComment : fileComments) {
            JHarmonizerOptOutMode currentMode = parseOptOutMode(fileComment);
            if (currentMode == null) {
                continue;
            }
            if (fileOptOutMode == null) {
                fileOptOutMode = currentMode;
                firstFileComment = fileComment;
                if (currentMode == JHarmonizerOptOutMode.FULLY_OFF) {
                    break;
                }
                continue;
            }
            if (currentMode == JHarmonizerOptOutMode.FULLY_OFF) {
                logIgnoredOptOut(
                        fileComment,
                        "File-scope fully-off overrides earlier sort-off from %s"
                                .formatted(formatLocation(firstFileComment.getPosition())));
                fileOptOutMode = JHarmonizerOptOutMode.FULLY_OFF;
                break;
            }
            logIgnoredOptOut(
                    fileComment,
                    "Conflicting file-scope opt-out; keeping the first one from %s"
                            .formatted(formatLocation(firstFileComment.getPosition())));
        }
        return fileOptOutMode;
    }

    private void collectTypeOptOutModes(
            CtType<?> currentType,
            boolean sortingDisabledInParents,
            Map<CtType<?>, JHarmonizerOptOutMode> typeOptOutModes) {
        JHarmonizerOptOutMode directOptOutMode = resolveDirectTypeOptOutMode(currentType);
        if (directOptOutMode == JHarmonizerOptOutMode.FULLY_OFF) {
            typeOptOutModes.put(currentType, JHarmonizerOptOutMode.FULLY_OFF);
            return;
        }

        boolean sortingDisabledForCurrentType = sortingDisabledInParents;
        if (directOptOutMode == JHarmonizerOptOutMode.SORTING_OFF && !sortingDisabledInParents) {
            typeOptOutModes.put(currentType, JHarmonizerOptOutMode.SORTING_OFF);
            sortingDisabledForCurrentType = true;
        } else if (directOptOutMode == JHarmonizerOptOutMode.SORTING_OFF) {
            sortingDisabledForCurrentType = true;
        }

        for (CtType<?> nestedType : currentType.getNestedTypes()) {
            collectTypeOptOutModes(nestedType, sortingDisabledForCurrentType, typeOptOutModes);
        }
    }

    @Nullable
    private JHarmonizerOptOutMode resolveDirectTypeOptOutMode(CtType<?> currentType) {
        List<CtComment> leadingTypeComments = currentType.getComments().stream()
                .filter(comment -> comment.getPosition().getEndLine()
                        < currentType.getPosition().getLine())
                .sorted(Comparator.comparingInt(comment -> comment.getPosition().getSourceStart()))
                .toList();
        JHarmonizerOptOutMode directOptOutMode = null;
        CtComment firstTypeComment = null;
        for (CtComment leadingTypeComment : leadingTypeComments) {
            JHarmonizerOptOutMode currentMode = parseOptOutMode(leadingTypeComment);
            if (currentMode == null) {
                continue;
            }
            if (directOptOutMode != null) {
                if (currentMode == JHarmonizerOptOutMode.FULLY_OFF
                        && directOptOutMode != JHarmonizerOptOutMode.FULLY_OFF) {
                    logIgnoredOptOut(
                            leadingTypeComment,
                            "Type '%s' fully-off overrides earlier sort-off from %s"
                                    .formatted(
                                            currentType.getQualifiedName(),
                                            formatLocation(firstTypeComment.getPosition())));
                    directOptOutMode = JHarmonizerOptOutMode.FULLY_OFF;
                    break;
                }
                logIgnoredOptOut(
                        leadingTypeComment,
                        "Duplicate opt-out for type '%s'; keeping the first one from %s"
                                .formatted(
                                        currentType.getQualifiedName(),
                                        formatLocation(firstTypeComment.getPosition())));
                continue;
            }
            directOptOutMode = currentMode;
            firstTypeComment = leadingTypeComment;
            if (directOptOutMode == JHarmonizerOptOutMode.FULLY_OFF) {
                break;
            }
        }
        return directOptOutMode;
    }

    @Nullable
    private JHarmonizerOptOutMode parseOptOutMode(CtComment comment) {
        if (comment.getCommentType() == CommentType.JAVADOC) {
            logIgnoredOptOut(comment, "Javadoc opt-out comments are ignored");
            return null;
        }

        String trimmedContent = comment.getContent().trim();
        String normalizedContent = trimmedContent.toLowerCase(Locale.ROOT);
        String normalizedTokenPrefix = JHarmonizerOptOutMode.TOKEN_PREFIX.toLowerCase(Locale.ROOT);
        int tokenPrefixIndex = normalizedContent.indexOf(normalizedTokenPrefix);
        if (tokenPrefixIndex < 0) {
            return null;
        }
        if (tokenPrefixIndex != 0) {
            logIgnoredOptOut(comment, "Malformed opt-out comment is ignored");
            return null;
        }

        try {
            return JHarmonizerOptOutMode.fromToken(normalizedContent);
        } catch (IllegalArgumentException exception) {
            logIgnoredOptOut(comment, exception.getMessage());
            return null;
        }
    }

    private static boolean isStandaloneComment(CtComment comment) {
        CtElement parent = comment.getParent();
        return !(parent instanceof CtType<?> || parent instanceof CtTypeMember);
    }

    private boolean isBeforeFirstDeclaredType(CtComment comment) {
        return comment.getPosition().getSourceEnd()
                < compilationUnit.getDeclaredTypes().stream()
                        .map(CtType::getPosition)
                        .mapToInt(SourcePosition::getSourceStart)
                        .min()
                        .orElse(Integer.MAX_VALUE);
    }

    private void logIgnoredOptOut(CtComment comment, String message) {
        if (log.isWarnEnabled()) {
            log.warn("{} at {}", message, formatLocation(comment.getPosition()));
        }
    }

    @NonNull
    private String formatLocation(SourcePosition sourcePosition) {
        return srcFile.getPath() + ":" + sourcePosition.getLine() + ":" + sourcePosition.getColumn();
    }
}
