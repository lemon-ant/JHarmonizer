package io.github.lemon_ant.jharmonizer.core.optout;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
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
@UtilityClass
@SuppressWarnings({
    "PMD.GuardLogStatement",
    "PMD.TooManyMethods",
    "PMD.CognitiveComplexity",
    "PMD.UseConcurrentHashMap",
    "PMD.AvoidInstantiatingObjectsInLoops"
})
public class JHarmonizerOptOutResolver {
    private static final String OPT_OUT_PREFIX = "@jharmonizer:";

    @NonNull
    public static JHarmonizerOptOuts resolve(
            @NonNull Path sourcePath, @NonNull String originalSourceCode, @NonNull CtCompilationUnit compilationUnit) {
        List<CtComment> potentialOptOutComments = findPotentialOptOutComments(compilationUnit);
        if (potentialOptOutComments.isEmpty()) {
            return JHarmonizerOptOuts.empty();
        }

        Map<SourcePosition, ResolvedJHarmonizerOptOut> typeOptOuts = new LinkedHashMap<>();
        ResolvedJHarmonizerOptOut fileOptOut = null;

        for (CtComment potentialOptOutComment : potentialOptOutComments) {
            Optional<JHarmonizerOptOutMode> optOutMode = parseOptOutMode(sourcePath, potentialOptOutComment);
            if (optOutMode.isEmpty()) {
                continue;
            }

            Optional<CtType<?>> targetType = findTypeTarget(compilationUnit, potentialOptOutComment);
            if (targetType.isPresent()) {
                ResolvedJHarmonizerOptOut resolvedOptOut = resolveTypeOptOut(
                        originalSourceCode, potentialOptOutComment, optOutMode.orElseThrow(), targetType.orElseThrow());
                ResolvedJHarmonizerOptOut previousOptOut =
                        typeOptOuts.putIfAbsent(targetType.orElseThrow().getPosition(), resolvedOptOut);
                if (previousOptOut != null) {
                    logIgnoredOptOut(
                            sourcePath,
                            potentialOptOutComment,
                            "Duplicate opt-out for type '%s'; keeping the first one from %s"
                                    .formatted(
                                            targetType.orElseThrow().getQualifiedName(),
                                            formatLocation(sourcePath, previousOptOut.getCommentPosition())));
                }
                continue;
            }

            if (isMemberLevelOptOut(compilationUnit, potentialOptOutComment)) {
                logIgnoredOptOut(
                        sourcePath, potentialOptOutComment, "Member-level JHarmonizer opt-out comments are ignored");
                continue;
            }

            if (isFileScopeOptOut(compilationUnit, potentialOptOutComment)) {
                ResolvedJHarmonizerOptOut resolvedOptOut = new ResolvedJHarmonizerOptOut(
                        potentialOptOutComment.getPosition(),
                        optOutMode.orElseThrow(),
                        null,
                        JHarmonizerOptOutScope.FILE,
                        null);
                if (fileOptOut != null) {
                    logIgnoredOptOut(
                            sourcePath,
                            potentialOptOutComment,
                            "Conflicting file-scope opt-out; keeping the first one from %s"
                                    .formatted(formatLocation(sourcePath, fileOptOut.getCommentPosition())));
                    continue;
                }
                fileOptOut = resolvedOptOut;
                continue;
            }

            logIgnoredOptOut(
                    sourcePath, potentialOptOutComment, "Opt-out comment is not in a supported file or type location");
        }

        return JHarmonizerOptOuts.of(fileOptOut, typeOptOuts);
    }

    @NonNull
    private static List<CtComment> findPotentialOptOutComments(CtCompilationUnit compilationUnit) {
        List<CtComment> potentialOptOutComments = new ArrayList<>();
        addPotentialOptOutComments(
                compilationUnit.getElements(new TypeFilter<>(CtComment.class)).stream()
                        .filter(comment -> !hasStructuredOwner(comment)),
                potentialOptOutComments);
        compilationUnit
                .getDeclaredTypes()
                .forEach(type -> collectPotentialOptOutComments(type, potentialOptOutComments));
        return List.copyOf(potentialOptOutComments);
    }

    private static void collectPotentialOptOutComments(CtType<?> type, List<CtComment> potentialOptOutComments) {
        addPotentialOptOutComments(type.getComments().stream(), potentialOptOutComments);
        for (CtTypeMember typeMember : type.getTypeMembers()) {
            addPotentialOptOutComments(typeMember.getComments().stream(), potentialOptOutComments);
            if (typeMember instanceof CtType<?> nestedType) {
                collectPotentialOptOutComments(nestedType, potentialOptOutComments);
            }
        }
    }

    private static void addPotentialOptOutComments(
            Stream<CtComment> comments, List<CtComment> potentialOptOutComments) {
        comments.filter(JHarmonizerOptOutResolver::isPotentialOptOutComment).forEach(potentialOptOutComments::add);
    }

    private static boolean hasStructuredOwner(CtComment comment) {
        CtElement parent = comment.getParent();
        return parent instanceof CtType<?> || parent instanceof CtTypeMember;
    }

    private static boolean isPotentialOptOutComment(CtComment comment) {
        return normalize(comment.getContent()).contains(OPT_OUT_PREFIX);
    }

    @NonNull
    private static Optional<JHarmonizerOptOutMode> parseOptOutMode(Path sourcePath, CtComment comment) {
        if (comment.getCommentType() == CommentType.JAVADOC) {
            logIgnoredOptOut(sourcePath, comment, "Javadoc opt-out comments are ignored");
            return Optional.empty();
        }

        String normalizedContent = normalize(comment.getContent()).trim();
        if (!normalizedContent.startsWith(OPT_OUT_PREFIX)) {
            logIgnoredOptOut(sourcePath, comment, "Malformed opt-out comment is ignored");
            return Optional.empty();
        }

        try {
            return Optional.of(JHarmonizerOptOutMode.fromToken(normalizedContent));
        } catch (IllegalArgumentException exception) {
            logIgnoredOptOut(sourcePath, comment, exception.getMessage());
            return Optional.empty();
        }
    }

    @NonNull
    private static Optional<CtType<?>> findTypeTarget(CtCompilationUnit compilationUnit, CtComment comment) {
        CtType<?> matchingType = compilationUnit.getDeclaredTypes().stream()
                .flatMap(type -> type.getElements(new TypeFilter<>(CtType.class)).stream())
                .map(type -> (CtType<?>) type)
                .filter(type -> hasSameLeadingComment(type.getComments(), comment))
                .filter(type ->
                        comment.getPosition().getEndLine() < type.getPosition().getLine())
                .findFirst()
                .orElse(null);
        return Optional.ofNullable(matchingType);
    }

    private static boolean isFileScopeOptOut(CtCompilationUnit compilationUnit, CtComment comment) {
        return comment.getPosition().getSourceEnd()
                < compilationUnit.getDeclaredTypes().stream()
                        .map(CtType::getPosition)
                        .mapToInt(SourcePosition::getSourceStart)
                        .min()
                        .orElse(Integer.MAX_VALUE);
    }

    private static boolean isMemberLevelOptOut(CtCompilationUnit compilationUnit, CtComment comment) {
        return compilationUnit.getDeclaredTypes().stream()
                .flatMap(type -> type.getElements(new TypeFilter<>(CtTypeMember.class)).stream())
                .filter(typeMember -> !(typeMember instanceof CtType<?>))
                .anyMatch(typeMember -> hasSameLeadingComment(typeMember.getComments(), comment)
                        && comment.getPosition().getEndLine()
                                < typeMember.getPosition().getLine());
    }

    @NonNull
    private static ResolvedJHarmonizerOptOut resolveTypeOptOut(
            String originalSourceCode, CtComment comment, JHarmonizerOptOutMode mode, CtType<?> targetType) {
        SourceCharacterRange preservedSourceRange = new SourceCharacterRange(
                findIndentationStart(originalSourceCode, comment.getPosition().getSourceStart()),
                targetType.getPosition().getSourceEnd() + 1);
        return new ResolvedJHarmonizerOptOut(
                comment.getPosition(),
                mode,
                preservedSourceRange,
                JHarmonizerOptOutScope.TYPE,
                ResolvedOptOutTargetType.from(targetType));
    }

    private static int findIndentationStart(String sourceCode, int startIndex) {
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

    private static boolean hasSameLeadingComment(List<CtComment> attachedComments, CtComment expectedComment) {
        return attachedComments.stream()
                .anyMatch(attachedComment -> hasSameSourceRange(attachedComment, expectedComment));
    }

    private static boolean hasSameSourceRange(CtComment leftComment, CtComment rightComment) {
        SourcePosition leftPosition = leftComment.getPosition();
        SourcePosition rightPosition = rightComment.getPosition();
        return leftPosition.getSourceStart() == rightPosition.getSourceStart()
                && leftPosition.getSourceEnd() == rightPosition.getSourceEnd();
    }

    private static void logIgnoredOptOut(Path sourcePath, CtComment comment, String message) {
        log.warn("{} at {}", message, formatLocation(sourcePath, comment.getPosition()));
    }

    @NonNull
    private static String formatLocation(Path sourcePath, SourcePosition sourcePosition) {
        return sourcePath + ":" + sourcePosition.getLine() + ":" + sourcePosition.getColumn();
    }

    @NonNull
    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT);
    }
}
