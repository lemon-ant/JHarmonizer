package io.github.lemon_ant.jharmonizer.core.directive;

import static java.util.Comparator.comparingInt;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtComment.CommentType;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.visitor.filter.TypeFilter;

@UtilityClass
public class JHarmonizerDirectiveResolver {
    private static final Pattern DIRECTIVE_PATTERN = Pattern.compile("@jharmonizer:[a-z-]+");

    @NonNull
    public static JHarmonizerDirectives resolve(
            @NonNull Path sourcePath, @NonNull String originalSourceCode, @NonNull CtCompilationUnit compilationUnit) {
        Set<CtComment> directiveComments = collectDirectiveComments(compilationUnit);
        if (directiveComments.isEmpty()) {
            return JHarmonizerDirectives.empty();
        }

        Map<SourcePosition, ResolvedJHarmonizerDirective> typeDirectives = new LinkedHashMap<>();
        ResolvedJHarmonizerDirective fileDirective = null;

        for (CtComment directiveComment : directiveComments.stream()
                .sorted(comparingInt(comment -> comment.getPosition().getSourceStart()))
                .toList()) {
            JHarmonizerDirectiveMode directiveMode = parseDirectiveMode(sourcePath, directiveComment);
            Optional<CtType<?>> targetType = findTypeTarget(compilationUnit, directiveComment);
            if (targetType.isPresent()) {
                ResolvedJHarmonizerDirective resolvedDirective =
                        resolveTypeDirective(originalSourceCode, directiveComment, directiveMode, targetType.get());
                ResolvedJHarmonizerDirective previousDirective =
                        typeDirectives.putIfAbsent(targetType.get().getPosition(), resolvedDirective);
                if (previousDirective != null) {
                    throw invalidDirective(
                            sourcePath,
                            directiveComment,
                            "Duplicate directives for type '%s': first at %s, second at %s"
                                    .formatted(
                                            targetType.get().getQualifiedName(),
                                            formatLocation(sourcePath, previousDirective.getDirectivePosition()),
                                            formatLocation(sourcePath, resolvedDirective.getDirectivePosition())));
                }
                continue;
            }

            if (isMemberLevelDirective(compilationUnit, directiveComment)) {
                throw invalidDirective(
                        sourcePath, directiveComment, "Member-level JHarmonizer directives are not supported");
            }

            if (isFileScopeDirective(compilationUnit, directiveComment)) {
                ResolvedJHarmonizerDirective resolvedDirective = new ResolvedJHarmonizerDirective(
                        DirectiveSourcePosition.from(directiveComment.getPosition()),
                        directiveMode,
                        null,
                        JHarmonizerDirectiveScope.FILE,
                        null);
                if (fileDirective != null) {
                    throw invalidDirective(
                            sourcePath,
                            directiveComment,
                            "Conflicting file-scope directives: first at %s, second at %s"
                                    .formatted(
                                            formatLocation(sourcePath, fileDirective.getDirectivePosition()),
                                            formatLocation(sourcePath, resolvedDirective.getDirectivePosition())));
                }
                fileDirective = resolvedDirective;
                continue;
            }

            throw invalidDirective(
                    sourcePath, directiveComment, "Directive is not in a supported file or type location");
        }

        return JHarmonizerDirectives.of(fileDirective, typeDirectives);
    }

    private static Set<CtComment> collectDirectiveComments(CtCompilationUnit compilationUnit) {
        Set<CtComment> directiveComments = new LinkedHashSet<>();
        directiveComments.addAll(compilationUnit.getElements(new TypeFilter<>(CtComment.class)));
        for (CtType<?> type : compilationUnit.getDeclaredTypes()) {
            collectDirectiveComments(type, directiveComments);
        }
        return directiveComments.stream()
                .filter(comment -> StringUtils.contains(comment.getContent(), "@jharmonizer:"))
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
    }

    private static void collectDirectiveComments(CtType<?> type, Collection<CtComment> directiveComments) {
        directiveComments.addAll(type.getComments());
        for (CtTypeMember typeMember : type.getTypeMembers()) {
            directiveComments.addAll(typeMember.getComments());
            if (typeMember instanceof CtType<?> nestedType) {
                collectDirectiveComments(nestedType, directiveComments);
            }
        }
    }

    @NonNull
    private static JHarmonizerDirectiveMode parseDirectiveMode(Path sourcePath, CtComment directiveComment) {
        if (directiveComment.getCommentType() == CommentType.JAVADOC) {
            throw invalidDirective(sourcePath, directiveComment, "Javadoc directives are not supported");
        }

        String trimmedContent = directiveComment.getContent().trim();
        Matcher matcher = DIRECTIVE_PATTERN.matcher(trimmedContent);
        if (!matcher.find()) {
            throw invalidDirective(sourcePath, directiveComment, "Directive token was not found");
        }
        if (!matcher.group().equals(trimmedContent)) {
            throw invalidDirective(
                    sourcePath,
                    directiveComment,
                    "Only exact directive comments are supported, but found: '" + trimmedContent + "'");
        }

        try {
            return JHarmonizerDirectiveMode.fromToken(trimmedContent);
        } catch (IllegalArgumentException exception) {
            throw invalidDirective(sourcePath, directiveComment, exception.getMessage());
        }
    }

    private static boolean isFileScopeDirective(CtCompilationUnit compilationUnit, CtComment directiveComment) {
        return directiveComment.getPosition().getSourceEnd() < findFirstTopLevelRenderStart(compilationUnit);
    }

    private static int findFirstTopLevelRenderStart(CtCompilationUnit compilationUnit) {
        return compilationUnit.getDeclaredTypes().stream()
                .map(CtType::getPosition)
                .mapToInt(SourcePosition::getSourceStart)
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    private static boolean isMemberLevelDirective(CtCompilationUnit compilationUnit, CtComment directiveComment) {
        return compilationUnit.getDeclaredTypes().stream()
                .flatMap(type -> type.getElements(new TypeFilter<>(CtTypeMember.class)).stream())
                .filter(typeMember -> !(typeMember instanceof CtType<?>))
                .anyMatch(typeMember -> typeMember.getComments().contains(directiveComment)
                        && directiveComment.getPosition().getEndLine()
                                < typeMember.getPosition().getLine());
    }

    @NonNull
    private static Optional<CtType<?>> findTypeTarget(CtCompilationUnit compilationUnit, CtComment directiveComment) {
        CtType<?> matchingType = compilationUnit.getDeclaredTypes().stream()
                .flatMap(type -> type.getElements(new TypeFilter<>(CtType.class)).stream())
                .map(type -> (CtType<?>) type)
                .filter(type -> type.getComments().contains(directiveComment))
                .filter(type -> directiveComment.getPosition().getEndLine()
                        < type.getPosition().getLine())
                .sorted(comparingInt(type -> type.getPosition().getSourceStart()))
                .findFirst()
                .orElse(null);
        return Optional.ofNullable(matchingType);
    }

    @NonNull
    private static ResolvedJHarmonizerDirective resolveTypeDirective(
            String originalSourceCode,
            CtComment directiveComment,
            JHarmonizerDirectiveMode directiveMode,
            CtType<?> targetType) {
        SourceCharacterRange preservedSourceRange = new SourceCharacterRange(
                findIndentationStart(
                        originalSourceCode, directiveComment.getPosition().getSourceStart()),
                targetType.getPosition().getSourceEnd() + 1);
        return new ResolvedJHarmonizerDirective(
                DirectiveSourcePosition.from(directiveComment.getPosition()),
                directiveMode,
                preservedSourceRange,
                JHarmonizerDirectiveScope.TYPE,
                ResolvedDirectiveTargetType.from(targetType));
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

    private static IllegalArgumentException invalidDirective(
            Path sourcePath, CtComment directiveComment, String message) {
        DirectiveSourcePosition sourcePosition = DirectiveSourcePosition.from(directiveComment.getPosition());
        return new IllegalArgumentException(message + " at " + formatLocation(sourcePath, sourcePosition));
    }

    private static String formatLocation(Path sourcePath, DirectiveSourcePosition sourcePosition) {
        return sourcePath + ":" + sourcePosition.getLine() + ":" + sourcePosition.getColumn();
    }
}
