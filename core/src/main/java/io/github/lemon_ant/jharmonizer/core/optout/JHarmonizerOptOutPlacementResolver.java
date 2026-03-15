package io.github.lemon_ant.jharmonizer.core.optout;

import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import spoon.reflect.code.CtComment;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.visitor.filter.TypeFilter;

@RequiredArgsConstructor
final class JHarmonizerOptOutPlacementResolver {
    @NonNull
    private final CtCompilationUnit compilationUnit;

    @NonNull
    Optional<CtType<?>> findTypeTarget(@NonNull CtComment comment) {
        CtType<?> matchingType = compilationUnit.getDeclaredTypes().stream()
                .flatMap(type -> type.getElements(new TypeFilter<>(CtType.class)).stream())
                .map(type -> (CtType<?>) type)
                .filter(type -> JHarmonizerOptOutCommentCollector.hasSameLeadingComment(type.getComments(), comment))
                .filter(type ->
                        comment.getPosition().getEndLine() < type.getPosition().getLine())
                .findFirst()
                .orElse(null);
        return Optional.ofNullable(matchingType);
    }

    boolean isFileScopeOptOut(@NonNull CtComment comment) {
        return comment.getPosition().getSourceEnd()
                < compilationUnit.getDeclaredTypes().stream()
                        .map(CtType::getPosition)
                        .mapToInt(SourcePosition::getSourceStart)
                        .min()
                        .orElse(Integer.MAX_VALUE);
    }

    boolean isMemberLevelOptOut(@NonNull CtComment comment) {
        return compilationUnit.getDeclaredTypes().stream()
                .flatMap(type -> type.getElements(new TypeFilter<>(CtTypeMember.class)).stream())
                .filter(typeMember -> !(typeMember instanceof CtType<?>))
                .anyMatch(typeMember ->
                        JHarmonizerOptOutCommentCollector.hasSameLeadingComment(typeMember.getComments(), comment)
                                && comment.getPosition().getEndLine()
                                        < typeMember.getPosition().getLine());
    }
}
