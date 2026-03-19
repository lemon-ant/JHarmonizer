package io.github.lemon_ant.jharmonizer.core.optout;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
                .filter(comment -> !hasStructuredOwner(comment))
                .filter(this::isPotentialOptOutComment)
                .filter(this::isFileScopeOptOut)
                .sorted(Comparator.comparingInt(comment -> comment.getPosition().getSourceStart()))
                .toList();
        JHarmonizerOptOutMode fileOptOutMode = null;
        CtComment firstFileComment = null;
        for (CtComment fileComment : fileComments) {
            Optional<JHarmonizerOptOutMode> parsedMode = parseOptOutMode(fileComment);
            if (parsedMode.isEmpty()) {
                continue;
            }
            JHarmonizerOptOutMode currentMode = parsedMode.orElseThrow();
            if (fileOptOutMode == null
                    || (fileOptOutMode == JHarmonizerOptOutMode.SORTING_OFF
                            && currentMode == JHarmonizerOptOutMode.FULLY_OFF)) {
                fileOptOutMode = currentMode;
                firstFileComment = fileComment;
                if (currentMode == JHarmonizerOptOutMode.FULLY_OFF) {
                    break;
                }
                continue;
            }
            if (fileOptOutMode == JHarmonizerOptOutMode.SORTING_OFF) {
                logIgnoredOptOut(
                        fileComment,
                        "Conflicting file-scope opt-out; keeping the first one from %s"
                                .formatted(formatLocation(firstFileComment.getPosition())));
            }
        }
        return fileOptOutMode;
    }

    private void collectTypeOptOutModes(
            @NonNull CtType<?> currentType,
            boolean sortingDisabledInParents,
            @NonNull Map<CtType<?>, JHarmonizerOptOutMode> typeOptOutModes) {
        JHarmonizerOptOutMode directOptOutMode = resolveDirectTypeOptOutMode(currentType, sortingDisabledInParents);
        if (directOptOutMode == JHarmonizerOptOutMode.FULLY_OFF) {
            typeOptOutModes.put(currentType, JHarmonizerOptOutMode.FULLY_OFF);
            return;
        }

        boolean sortingDisabledForNestedTypes = sortingDisabledInParents;
        if (directOptOutMode == JHarmonizerOptOutMode.SORTING_OFF) {
            typeOptOutModes.put(currentType, JHarmonizerOptOutMode.SORTING_OFF);
            sortingDisabledForNestedTypes = true;
        }

        for (CtType<?> nestedType : currentType.getNestedTypes()) {
            collectTypeOptOutModes(nestedType, sortingDisabledForNestedTypes, typeOptOutModes);
        }
    }

    @Nullable
    private JHarmonizerOptOutMode resolveDirectTypeOptOutMode(
            @NonNull CtType<?> currentType, boolean sortingDisabledInParents) {
        List<CtComment> leadingTypeComments = currentType.getComments().stream()
                .filter(this::isPotentialOptOutComment)
                .filter(comment -> comment.getPosition().getEndLine()
                        < currentType.getPosition().getLine())
                .sorted(Comparator.comparingInt(comment -> comment.getPosition().getSourceStart()))
                .toList();
        JHarmonizerOptOutMode directOptOutMode = null;
        CtComment firstTypeComment = null;
        for (CtComment leadingTypeComment : leadingTypeComments) {
            Optional<JHarmonizerOptOutMode> parsedMode = parseOptOutMode(leadingTypeComment);
            if (parsedMode.isEmpty()) {
                continue;
            }
            JHarmonizerOptOutMode currentMode = parsedMode.orElseThrow();
            if (sortingDisabledInParents && currentMode == JHarmonizerOptOutMode.SORTING_OFF) {
                continue;
            }
            if (directOptOutMode == JHarmonizerOptOutMode.SORTING_OFF
                    && currentMode == JHarmonizerOptOutMode.FULLY_OFF) {
                directOptOutMode = JHarmonizerOptOutMode.FULLY_OFF;
                firstTypeComment = leadingTypeComment;
                break;
            }
            if (directOptOutMode != null) {
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

    @NonNull
    private Optional<JHarmonizerOptOutMode> parseOptOutMode(@NonNull CtComment comment) {
        if (comment.getCommentType() == CommentType.JAVADOC) {
            logIgnoredOptOut(comment, "Javadoc opt-out comments are ignored");
            return Optional.empty();
        }

        String normalizedContent = comment.getContent().trim().toLowerCase(java.util.Locale.ROOT);
        String normalizedTokenPrefix = JHarmonizerOptOutMode.TOKEN_PREFIX.toLowerCase(java.util.Locale.ROOT);
        if (!normalizedContent.startsWith(normalizedTokenPrefix)) {
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

    private boolean isPotentialOptOutComment(@NonNull CtComment comment) {
        return StringUtils.containsIgnoreCase(comment.getContent(), JHarmonizerOptOutMode.TOKEN_PREFIX);
    }

    private boolean hasStructuredOwner(@NonNull CtComment comment) {
        CtElement parent = comment.getParent();
        return parent instanceof CtType<?> || parent instanceof CtTypeMember;
    }

    private boolean isFileScopeOptOut(@NonNull CtComment comment) {
        return comment.getPosition().getSourceEnd()
                < compilationUnit.getDeclaredTypes().stream()
                        .map(CtType::getPosition)
                        .mapToInt(SourcePosition::getSourceStart)
                        .min()
                        .orElse(Integer.MAX_VALUE);
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
