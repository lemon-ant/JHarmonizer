package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.translator.SerializedSrcWithSkippedTypeRanges;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SpoonCustomSrcPrinterTest {

    @Test
    void serializeCompilationUnit_afterSkippedRangesHandedOff_throwIllegalStateException() {
        // Given
        String srcCode = """
                class Alpha {}

                // @jharmonizer:sort-off
                class Beta { int z; int a; }
                """;
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Beta.java")));
        SpoonCustomSrcPrinter printer = new SpoonCustomSrcPrinter(
                spoonAstModel.getCompilationUnit().getFactory().getEnvironment(),
                srcCode,
                spoonAstModel.getOptOuts().getSortingSkippedTypes());

        // When
        SerializedSrcWithSkippedTypeRanges serializedSrcWithSkippedTypeRanges =
                printer.serializeCompilationUnit(spoonAstModel.getCompilationUnit());

        // Then
        assertThat(serializedSrcWithSkippedTypeRanges.getSortingSkippedTypeRanges())
                .hasSize(1);
        assertThatThrownBy(() -> printer.serializeCompilationUnit(spoonAstModel.getCompilationUnit()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been finalized");
    }

    @Test
    void serializeCompilationUnit_enumConstantsWithClassBodies_keepMembersOutsideConstantBody() {
        // Given
        String srcCode = """
                enum Metric {
                    FIRST {
                        @Override
                        long value() {
                            return 1L;
                        }
                    },
                    SECOND {
                        @Override
                        long value() {
                            return 2L;
                        }
                    };

                    private final long cached = 7L;

                    abstract long value();

                    long total() {
                        return value() + cached;
                    }
                }
                """;
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Metric.java")));
        SpoonCustomSrcPrinter printer = new SpoonCustomSrcPrinter(
                spoonAstModel.getCompilationUnit().getFactory().getEnvironment(),
                srcCode,
                spoonAstModel.getOptOuts().getSortingSkippedTypes());

        // When
        SerializedSrcWithSkippedTypeRanges serializedSrcWithSkippedTypeRanges =
                printer.serializeCompilationUnit(spoonAstModel.getCompilationUnit());

        // Then
        String serializedSrc = serializedSrcWithSkippedTypeRanges.getSerializedSrcCode();
        assertThat(serializedSrc).contains("};");
        assertThat(serializedSrc).contains("private final long cached = 7L;");
        assertThat(serializedSrc.indexOf("};"))
                .isLessThan(serializedSrc.indexOf("private final long cached = 7L;"));
        assertThatCodeParses(serializedSrc);
    }

    private static void assertThatCodeParses(String srcCode) {
        SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Metric.java")));
    }
}
