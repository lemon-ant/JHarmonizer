// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import static io.github.lemon_ant.jharmonizer.core.testutils.JavaCompileTestUtils.compileJavaSourcesWithRelease21;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseAstModelFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathDirectoryUrl;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.resolveRelativeUrl;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.testutils.JavaCompileTestUtils;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import spoon.reflect.declaration.CtCompilationUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ModuleAnnotationTypeNameResolverTest {

    @NonNull
    private static final URL FIXTURE_ROOT =
            requireClasspathDirectoryUrl("/test-cases/core/spoon/module-annotation-context/package-owner/");

    @NonNull
    private AnnotationDependencyKeyResolver dependencyKeyResolver;

    @NonNull
    private SpoonAstModel moduleModel;

    @NonNull
    private AnnotationTypeNameResolver moduleResolver;

    // Parameter injection supplies the directory before @BeforeAll; instance @TempDir fields are prepared later.
    @BeforeAll
    void setUp(@TempDir @NonNull Path temporaryDirectory) throws Exception {
        Path moduleInfoSrc = copyFixtureSrc(temporaryDirectory, "module-info.java");
        Path annotationSrc = copyFixtureSrc(temporaryDirectory, "Owner/Tag.java");
        JavaCompileTestUtils.CompileResult compileResult = compileJavaSourcesWithRelease21(
                List.of(moduleInfoSrc, annotationSrc), temporaryDirectory.resolve("classes"));
        assertThat(compileResult.getExitCode())
                .as("Expected the complete module fixture to compile. Diagnostics:%n%s", compileResult.getOutput())
                .isZero();
        // Parse only the original descriptor, matching production's single-file model.
        moduleModel = parseAstModelFromJavaFixtureResource(moduleInfoSrc.toUri().toURL());
        CtCompilationUnit compilationUnit = moduleModel.getCompilationUnit();
        SpoonTypeImportContext typeImportContext = SpoonImportUtils.collectTypeImportContext(compilationUnit, Map.of());
        moduleResolver = new AnnotationTypeNameResolver(compilationUnit, typeImportContext);
        dependencyKeyResolver = new AnnotationDependencyKeyResolver(compilationUnit);
    }

    @Test
    void resolveAnnotationTypeName_modulePackageOwner_retainsUnconfirmedWrittenName() {
        // Given
        String writtenAnnotationTypeName = "Owner.Tag";
        int srcOffset = moduleModel.getSrcCode().indexOf("@" + writtenAnnotationTypeName);

        // When
        AnnotationTypeNameResolution resolution =
                moduleResolver.resolveAnnotationTypeName(srcOffset, writtenAnnotationTypeName);
        String dependencyKey = dependencyKeyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(
                srcOffset, writtenAnnotationTypeName);

        // Then
        assertThat(moduleModel.getCompilationUnit().getImports()).isEmpty();
        assertThat(moduleModel.getCompilationUnit().getDeclaredTypes()).isEmpty();
        assertThat(moduleModel.getCompilationUnit().getDeclaredModule().getAnnotations())
                .as("Spoon 11.5.0 omits the original module annotation use")
                .isEmpty();
        assertThat(moduleModel.getAnnotationSrcGroups().stream()
                        .flatMap(annotationGroup -> annotationGroup.getAnnotationSrcFragmentsInSrcOrder().stream()))
                .anyMatch(annotationFragment -> annotationFragment.getAnnotationStart() == srcOffset);
        assertThat(resolution.getAnnotationTypeName()).isEqualTo(writtenAnnotationTypeName);
        assertThat(resolution.isCanonicalTypeNameConfirmed()).isFalse();
        assertThat(dependencyKey).isEqualTo("?.Owner.Tag");
    }

    @NonNull
    private static Path copyFixtureSrc(Path temporaryDirectory, String relativeSrcPath) throws IOException {
        Path copiedSrcFile = temporaryDirectory.resolve("src").resolve(relativeSrcPath);
        Files.createDirectories(copiedSrcFile.getParent());
        try (InputStream fixtureStream =
                resolveRelativeUrl(FIXTURE_ROOT, relativeSrcPath).openStream()) {
            Files.copy(fixtureStream, copiedSrcFile);
        }
        return copiedSrcFile;
    }
}
