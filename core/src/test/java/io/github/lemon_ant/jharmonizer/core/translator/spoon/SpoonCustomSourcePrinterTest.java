package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.translator.SerializedSourceWithSkippedTypeRanges;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SpoonCustomSourcePrinterTest {

    @Test
    void serializeCompilationUnit_afterSkippedRangesHandedOff_throwIllegalStateException() {
        // Given
        String sourceCode = """
                class Alpha {}

                // @jharmonizer:sort-off
                class Beta { int z; int a; }
                """;
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(sourceCode, Path.of("Beta.java")));
        SpoonCustomSourcePrinter printer = new SpoonCustomSourcePrinter(
                spoonAstModel.getCompilationUnit().getFactory().getEnvironment(),
                sourceCode,
                spoonAstModel.getOptOuts().getSortingSkippedTypes());

        // When
        SerializedSourceWithSkippedTypeRanges serializedSourceWithSkippedTypeRanges =
                printer.serializeCompilationUnit(spoonAstModel.getCompilationUnit());

        // Then
        assertThat(serializedSourceWithSkippedTypeRanges.getSortingSkippedTypeRanges())
                .hasSize(1);
        assertThatThrownBy(() -> printer.serializeCompilationUnit(spoonAstModel.getCompilationUnit()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been finalized");
    }
}
