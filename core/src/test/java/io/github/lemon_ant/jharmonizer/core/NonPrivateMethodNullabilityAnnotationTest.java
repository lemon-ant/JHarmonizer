package io.github.lemon_ant.jharmonizer.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.reflect.code.CtLambdaImpl;

class NonPrivateMethodNullabilityAnnotationTest {

    @Test
    void nonPrivateMethods_referenceTypesAnnotated_reportsNoViolations() {
        // Given
        Path coreModuleRoot = Path.of("").toAbsolutePath().normalize();
        Path repositoryRoot = coreModuleRoot.getParent();
        List<Path> sourceDirectories =
                List.of(coreModuleRoot.resolve("src/main/java"), repositoryRoot.resolve("cli/src/main/java"));

        // When
        List<String> violations = collectViolations(sourceDirectories, repositoryRoot);

        // Then
        assertThat(violations)
                .as(
                        "All non-private methods must declare explicit nullability for reference-type parameters and return values")
                .isEmpty();
    }

    private static List<String> collectViolations(List<Path> sourceDirectories, Path repositoryRoot) {
        CtModel model = buildModel(sourceDirectories);
        List<String> violations = new ArrayList<>();

        for (CtMethod<?> method : model.getElements(new TypeFilter<>(CtMethod.class))) {
            collectMethodViolations(violations, repositoryRoot, method);
        }

        return violations.stream().sorted().toList();
    }

    private static CtModel buildModel(List<Path> sourceDirectories) {
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
        sourceDirectories.forEach(sourceDirectory -> launcher.addInputResource(sourceDirectory.toString()));
        launcher.buildModel();
        return launcher.getModel();
    }

    private static void collectMethodViolations(List<String> violations, Path repositoryRoot, CtMethod<?> method) {
        if (method.isImplicit()
                || method.getParent(CtLambdaImpl.class) != null
                || method.hasModifier(ModifierKind.PRIVATE)) {
            return;
        }

        String relativeLocation =
                repositoryRoot.relativize(method.getPosition().getFile().toPath()) + ":"
                        + method.getPosition().getLine();
        String methodSignature = method.getDeclaringType().getQualifiedName() + "#" + method.getSignature();

        if (!isVoidOrPrimitive(method.getType())
                && !hasAllowedNullability(method.getType().getAnnotations())) {
            violations.add(relativeLocation + " RETURN " + methodSignature);
        }

        for (CtParameter<?> parameter : method.getParameters()) {
            if (isPrimitive(parameter.getType())
                    || hasAllowedNullability(parameter.getAnnotations())
                    || hasAllowedNullability(parameter.getType().getAnnotations())) {
                continue;
            }

            violations.add(relativeLocation + " PARAM " + methodSignature + " :: " + parameter.getSimpleName());
        }
    }

    private static boolean isVoidOrPrimitive(CtTypeReference<?> typeReference) {
        return typeReference == null || "void".equals(typeReference.getSimpleName()) || typeReference.isPrimitive();
    }

    private static boolean isPrimitive(CtTypeReference<?> typeReference) {
        return typeReference != null && typeReference.isPrimitive();
    }

    private static boolean hasAllowedNullability(List<CtAnnotation<?>> annotations) {
        return annotations.stream()
                .map(CtAnnotation::getAnnotationType)
                .filter(annotationType -> annotationType != null)
                .map(CtTypeReference::getQualifiedName)
                .anyMatch(Constants.ALLOWED_NULLABILITY_ANNOTATIONS::contains);
    }

    private static final class Constants {
        private static final Set<String> ALLOWED_NULLABILITY_ANNOTATIONS = Set.of(
                "lombok.NonNull",
                "edu.umd.cs.findbugs.annotations.Nullable",
                "javax.annotation.Nullable",
                "javax.annotation.Nonnull");

        private Constants() {}
    }
}
