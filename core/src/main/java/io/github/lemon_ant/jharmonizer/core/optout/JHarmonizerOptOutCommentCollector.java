package io.github.lemon_ant.jharmonizer.core.optout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import spoon.reflect.code.CtComment;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.visitor.filter.TypeFilter;

@UtilityClass
class JHarmonizerOptOutCommentCollector {
    private static final String OPT_OUT_PREFIX = "@jharmonizer:";

    @NonNull
    static List<CtComment> findPotentialOptOutComments(@NonNull CtCompilationUnit compilationUnit) {
        List<CtComment> potentialOptOutComments = new ArrayList<>();
        addPotentialOptOutComments(
                compilationUnit.getElements(new TypeFilter<>(CtComment.class)).stream()
                        .filter(comment -> !hasStructuredOwner(comment)),
                potentialOptOutComments);
        compilationUnit
                .getDeclaredTypes()
                .forEach(type -> collectPotentialOptOutComments(type, potentialOptOutComments));
        return Collections.unmodifiableList(potentialOptOutComments);
    }

    static boolean hasSameLeadingComment(
            @NonNull List<CtComment> attachedComments, @NonNull CtComment expectedComment) {
        return attachedComments.stream()
                .anyMatch(attachedComment -> hasSameSourceRange(attachedComment, expectedComment));
    }

    private static void collectPotentialOptOutComments(
            @NonNull CtType<?> type, @NonNull List<CtComment> potentialOptOutComments) {
        addPotentialOptOutComments(type.getComments().stream(), potentialOptOutComments);
        for (CtTypeMember typeMember : type.getTypeMembers()) {
            addPotentialOptOutComments(typeMember.getComments().stream(), potentialOptOutComments);
            if (typeMember instanceof CtType<?> nestedType) {
                collectPotentialOptOutComments(nestedType, potentialOptOutComments);
            }
        }
    }

    private static void addPotentialOptOutComments(
            @NonNull Stream<CtComment> comments, @NonNull List<CtComment> potentialOptOutComments) {
        comments.filter(JHarmonizerOptOutCommentCollector::isPotentialOptOutComment)
                .forEach(potentialOptOutComments::add);
    }

    private static boolean isPotentialOptOutComment(@NonNull CtComment comment) {
        return StringUtils.containsIgnoreCase(comment.getContent(), OPT_OUT_PREFIX);
    }

    private static boolean hasStructuredOwner(@NonNull CtComment comment) {
        CtElement parent = comment.getParent();
        return parent instanceof CtType<?> || parent instanceof CtTypeMember;
    }

    private static boolean hasSameSourceRange(@NonNull CtComment leftComment, @NonNull CtComment rightComment) {
        return leftComment.getPosition().getSourceStart()
                        == rightComment.getPosition().getSourceStart()
                && leftComment.getPosition().getSourceEnd()
                        == rightComment.getPosition().getSourceEnd();
    }
}
