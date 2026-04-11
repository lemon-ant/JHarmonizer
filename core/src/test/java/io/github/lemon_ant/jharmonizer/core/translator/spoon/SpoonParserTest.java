package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.translator.SpoonModelBuildException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import spoon.Launcher;

class SpoonParserTest {

    @Test
    void buildSpoonAstModel_launcherBuildFails_wrapsWithSpoonModelBuildException() throws Exception {
        // Given
        RuntimeException launcherFailure = new RuntimeException("boom");
        Launcher launcher = mock(Launcher.class);
        doThrow(launcherFailure).when(launcher).buildModel();
        SrcFile srcFile = createSrcFile("class Sample {}", Path.of("Sample.java"));

        // When / Then
        assertThatThrownBy(() -> invokeBuildSpoonAstModel(srcFile, launcher))
                .isInstanceOf(SpoonModelBuildException.class)
                .satisfies(throwable -> {
                    SpoonModelBuildException spoonModelBuildException = (SpoonModelBuildException) throwable;
                    assertThat(spoonModelBuildException.getSrcPath()).isEqualTo(Path.of("Sample.java"));
                    assertThat(spoonModelBuildException)
                            .hasMessageContaining("RuntimeException")
                            .hasMessageContaining("boom")
                            .hasCause(launcherFailure);
                });
    }

    private static SpoonAstModel invokeBuildSpoonAstModel(@NonNull SrcFile srcFile, @NonNull Launcher launcher)
            throws Exception {
        Method buildSpoonAstModel = SpoonParser.class.getDeclaredMethod(
                "buildSpoonAstModel", SrcFile.class, Launcher.class, PrinterConfig.class);
        buildSpoonAstModel.setAccessible(true);
        try {
            return (SpoonAstModel) buildSpoonAstModel.invoke(null, srcFile, launcher, new PrinterConfig(true, true));
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }
}
