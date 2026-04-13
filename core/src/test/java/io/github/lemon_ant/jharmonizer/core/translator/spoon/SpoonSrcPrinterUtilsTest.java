package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class SpoonSrcPrinterUtilsTest {

    private static final Path SAMPLE_PATH = Path.of("Sample.java");

    @Nested
    class DetectDominantLineSeparator {

        @Test
        void detectDominantLineSeparator_emptySource_returnsSystemLineSeparator() {
            // When
            String lineSeparator = SpoonSrcPrinterUtils.detectDominantLineSeparator("");

            // Then
            assertThat(lineSeparator).isEqualTo(System.lineSeparator());
        }

        @Test
        void detectDominantLineSeparator_noLineSeparators_returnsSystemLineSeparator() {
            // When
            String lineSeparator = SpoonSrcPrinterUtils.detectDominantLineSeparator("class A { int x; }");

            // Then
            assertThat(lineSeparator).isEqualTo(System.lineSeparator());
        }

        @Test
        void detectDominantLineSeparator_lfOnly_returnsLf() {
            // When
            String lineSeparator = SpoonSrcPrinterUtils.detectDominantLineSeparator("line1\nline2\nline3");

            // Then
            assertThat(lineSeparator).isEqualTo("\n");
        }

        @Test
        void detectDominantLineSeparator_crlfOnly_returnsCrlf() {
            // When
            String lineSeparator = SpoonSrcPrinterUtils.detectDominantLineSeparator("line1\r\nline2\r\nline3");

            // Then
            assertThat(lineSeparator).isEqualTo("\r\n");
        }

        @Test
        void detectDominantLineSeparator_crOnly_returnsCr() {
            // When
            String lineSeparator = SpoonSrcPrinterUtils.detectDominantLineSeparator("line1\rline2\rline3");

            // Then
            assertThat(lineSeparator).isEqualTo("\r");
        }

        @Test
        void detectDominantLineSeparator_crlfDominant_returnsCrlf() {
            // Given
            String source = "a\r\nb\r\nc\r\nd\ne\nf\rg";

            // When
            String lineSeparator = SpoonSrcPrinterUtils.detectDominantLineSeparator(source);

            // Then
            assertThat(lineSeparator).isEqualTo("\r\n");
        }

        @Test
        void detectDominantLineSeparator_lfDominant_returnsLf() {
            // Given
            String source = "a\nb\nc\nd\ne\r\nf\rg";

            // When
            String lineSeparator = SpoonSrcPrinterUtils.detectDominantLineSeparator(source);

            // Then
            assertThat(lineSeparator).isEqualTo("\n");
        }

        @Test
        void detectDominantLineSeparator_crDominant_returnsCr() {
            // Given
            String source = "a\rb\rc\rd\re\nf\r\ng";

            // When
            String lineSeparator = SpoonSrcPrinterUtils.detectDominantLineSeparator(source);

            // Then
            assertThat(lineSeparator).isEqualTo("\r");
        }

        @Test
        void detectDominantLineSeparator_crlfTieLfFallback_returnsCrlf() {
            // Given: equal CRLF and LF counts, CRLF should win (CRLF >= LF)
            String source = "a\r\nb\nc";

            // When
            String lineSeparator = SpoonSrcPrinterUtils.detectDominantLineSeparator(source);

            // Then
            assertThat(lineSeparator).isEqualTo("\r\n");
        }

        @Test
        void detectDominantLineSeparator_lfTieCrFallback_returnsLf() {
            // Given: equal LF and CR counts, LF should win (LF >= CR)
            String source = "a\nb\rc";

            // When
            String lineSeparator = SpoonSrcPrinterUtils.detectDominantLineSeparator(source);

            // Then
            assertThat(lineSeparator).isEqualTo("\n");
        }
    }

    @Nested
    class CompileNeedsSeparatorAfter {

        @Test
        void compileNeedsSeparatorAfter_blankLineBetweenFieldsEnabled_alwaysReturnsTrue() {
            // Given
            PrinterConfig printerConfig = new PrinterConfig(false, false, true);
            Predicate<CtTypeMember> needsSeparatorAfter =
                    SpoonSrcPrinterUtils.compileNeedsSeparatorAfter(printerConfig);
            SpoonAstModel spoonAstModel =
                    SpoonParser.parseJavaSrcFile(createSrcFile("class Sample { int x; }", SAMPLE_PATH), printerConfig);
            CtTypeMember field = spoonAstModel
                    .getCompilationUnit()
                    .getDeclaredTypes()
                    .getFirst()
                    .getTypeMembers()
                    .getFirst();

            // When
            boolean needsSeparator = needsSeparatorAfter.test(field);

            // Then
            assertThat(needsSeparator).isTrue();
        }

        @Test
        void compileNeedsSeparatorAfter_blankLineBetweenFieldsDisabled_fieldNoAnnotations_returnsFalse() {
            // Given
            PrinterConfig printerConfig = new PrinterConfig(false, false, false);
            Predicate<CtTypeMember> needsSeparatorAfter =
                    SpoonSrcPrinterUtils.compileNeedsSeparatorAfter(printerConfig);
            SpoonAstModel spoonAstModel =
                    SpoonParser.parseJavaSrcFile(createSrcFile("class Sample { int x; }", SAMPLE_PATH), printerConfig);
            CtTypeMember field =
                    spoonAstModel.getCompilationUnit().getDeclaredTypes().getFirst().getTypeMembers().stream()
                            .filter(member -> member instanceof CtField)
                            .findFirst()
                            .orElseThrow();

            // When
            boolean needsSeparator = needsSeparatorAfter.test(field);

            // Then
            assertThat(needsSeparator).isFalse();
        }

        @Test
        void compileNeedsSeparatorAfter_blankLineBetweenFieldsDisabled_nonField_returnsTrue() {
            // Given
            PrinterConfig printerConfig = new PrinterConfig(false, false, false);
            Predicate<CtTypeMember> needsSeparatorAfter =
                    SpoonSrcPrinterUtils.compileNeedsSeparatorAfter(printerConfig);
            SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class Sample { void foo() {} }", SAMPLE_PATH), printerConfig);
            CtTypeMember method =
                    spoonAstModel.getCompilationUnit().getDeclaredTypes().getFirst().getTypeMembers().stream()
                            .filter(member -> member instanceof CtMethod)
                            .findFirst()
                            .orElseThrow();

            // When
            boolean needsSeparator = needsSeparatorAfter.test(method);

            // Then
            assertThat(needsSeparator).isTrue();
        }
    }

    @Nested
    class CompileNeedsSeparatorBefore {

        @Test
        void compileNeedsSeparatorBefore_blankLineBeforeCommentEnabled_returnsPredicate() {
            // Given
            PrinterConfig printerConfig = new PrinterConfig(false, true, false);

            // When
            BiPredicate<CtTypeMember, Boolean> needsSeparatorBefore =
                    SpoonSrcPrinterUtils.compileNeedsSeparatorBefore(printerConfig);

            // Then
            assertThat(needsSeparatorBefore).isNotNull();
        }

        @Test
        void compileNeedsSeparatorBefore_blankLineBeforeCommentDisabled_returnsPredicate() {
            // Given
            PrinterConfig printerConfig = new PrinterConfig(false, false, false);

            // When
            BiPredicate<CtTypeMember, Boolean> needsSeparatorBefore =
                    SpoonSrcPrinterUtils.compileNeedsSeparatorBefore(printerConfig);

            // Then
            assertThat(needsSeparatorBefore).isNotNull();
        }

        @Test
        void compileNeedsSeparatorBefore_notFirstNonFieldMember_returnsTrue() {
            // Given
            PrinterConfig printerConfig = new PrinterConfig(false, false, false);
            BiPredicate<CtTypeMember, Boolean> needsSeparatorBefore =
                    SpoonSrcPrinterUtils.compileNeedsSeparatorBefore(printerConfig);
            SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class Sample { void foo() {} void bar() {} }", SAMPLE_PATH), printerConfig);
            CtTypeMember secondMethod = spoonAstModel
                    .getCompilationUnit()
                    .getDeclaredTypes()
                    .getFirst()
                    .getTypeMembers()
                    .get(1);

            // When
            boolean needsSeparator = needsSeparatorBefore.test(secondMethod, false);

            // Then
            assertThat(needsSeparator).isTrue();
        }

        @Test
        void compileNeedsSeparatorBefore_firstFieldMember_returnsFalse() {
            // Given
            PrinterConfig printerConfig = new PrinterConfig(false, false, false);
            BiPredicate<CtTypeMember, Boolean> needsSeparatorBefore =
                    SpoonSrcPrinterUtils.compileNeedsSeparatorBefore(printerConfig);
            SpoonAstModel spoonAstModel =
                    SpoonParser.parseJavaSrcFile(createSrcFile("class Sample { int x; }", SAMPLE_PATH), printerConfig);
            CtTypeMember field = spoonAstModel
                    .getCompilationUnit()
                    .getDeclaredTypes()
                    .getFirst()
                    .getTypeMembers()
                    .getFirst();

            // When
            boolean needsSeparator = needsSeparatorBefore.test(field, true);

            // Then
            assertThat(needsSeparator).isFalse();
        }
    }

    @Nested
    class CompileNeedsBlankLineAfterTypeHeader {

        @Test
        void compileNeedsBlankLineAfterTypeHeader_flagEnabled_alwaysReturnsTrue() {
            // Given
            PrinterConfig printerConfig = new PrinterConfig(true, false, false);
            Predicate<CtType<?>> needsBlankLine =
                    SpoonSrcPrinterUtils.compileNeedsBlankLineAfterTypeHeader(printerConfig);
            SpoonAstModel spoonAstModel =
                    SpoonParser.parseJavaSrcFile(createSrcFile("class Sample { int x; }", SAMPLE_PATH), printerConfig);
            CtType<?> sampleType =
                    spoonAstModel.getCompilationUnit().getDeclaredTypes().getFirst();

            // When
            boolean needsBlankLineAfterHeader = needsBlankLine.test(sampleType);

            // Then
            assertThat(needsBlankLineAfterHeader).isTrue();
        }

        @Test
        void compileNeedsBlankLineAfterTypeHeader_flagDisabledRegularClass_returnsFalse() {
            // Given
            PrinterConfig printerConfig = new PrinterConfig(false, false, false);
            Predicate<CtType<?>> needsBlankLine =
                    SpoonSrcPrinterUtils.compileNeedsBlankLineAfterTypeHeader(printerConfig);
            SpoonAstModel spoonAstModel =
                    SpoonParser.parseJavaSrcFile(createSrcFile("class Sample { int x; }", SAMPLE_PATH), printerConfig);
            CtType<?> sampleType =
                    spoonAstModel.getCompilationUnit().getDeclaredTypes().getFirst();

            // When
            boolean needsBlankLineAfterHeader = needsBlankLine.test(sampleType);

            // Then
            assertThat(needsBlankLineAfterHeader).isFalse();
        }

        @Test
        void compileNeedsBlankLineAfterTypeHeader_flagDisabledEnum_returnsTrue() {
            // Given
            PrinterConfig printerConfig = new PrinterConfig(false, false, false);
            Predicate<CtType<?>> needsBlankLine =
                    SpoonSrcPrinterUtils.compileNeedsBlankLineAfterTypeHeader(printerConfig);
            SpoonAstModel spoonAstModel =
                    SpoonParser.parseJavaSrcFile(createSrcFile("enum Day { MON, TUE; }", SAMPLE_PATH), printerConfig);
            CtType<?> enumType =
                    spoonAstModel.getCompilationUnit().getDeclaredTypes().getFirst();

            // When
            boolean needsBlankLineAfterHeader = needsBlankLine.test(enumType);

            // Then
            assertThat(needsBlankLineAfterHeader).isTrue();
        }
    }
}
