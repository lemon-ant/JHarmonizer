package io.github.lemon_ant.jharmonizer.core.translator;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ParsingResultTest {

    private static final PrinterConfig DEFAULT_PRINTER_CONFIG = new PrinterConfig(true, true, false);

    @Test
    void equals_sameInstance_returnsTrue() {
        // Given
        ParsingResult parsingResult = parseSimpleClass();

        // When / Then
        assertThat(parsingResult).isEqualTo(parsingResult);
    }

    @Test
    void equals_nullObject_returnsFalse() {
        // Given
        ParsingResult parsingResult = parseSimpleClass();

        // When / Then
        assertThat(parsingResult).isNotEqualTo(null);
    }

    @Test
    void equals_differentClass_returnsFalse() {
        // Given
        ParsingResult parsingResult = parseSimpleClass();

        // When / Then
        assertThat(parsingResult).isNotEqualTo("not a parsing result");
    }

    @Test
    void equals_equalParsingResults_returnsTrue() {
        // Given
        ParsingResult first = parseSimpleClass();
        SpoonAstModel sharedModel = first.getSpoonAstModel();
        ParsingResult second = new ParsingResult(first.getParsingStatistic(), sharedModel);

        // When / Then
        assertThat(first).isEqualTo(second);
    }

    @Test
    void equals_differentSpoonAstModel_returnsFalse() {
        // Given
        ParsingResult first = parseSimpleClass();
        ParsingResult second = parseOtherClass();

        // When / Then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void hashCode_equalObjects_produceSameHashCode() {
        // Given
        ParsingResult first = parseSimpleClass();
        SpoonAstModel sharedModel = first.getSpoonAstModel();
        ParsingResult second = new ParsingResult(first.getParsingStatistic(), sharedModel);

        // When / Then
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    private static ParsingResult parseSimpleClass() {
        return SrcAstTranslator.parse(
                createSrcFile("class SimpleClass { int x; }", Path.of("SimpleClass.java")), DEFAULT_PRINTER_CONFIG);
    }

    private static ParsingResult parseOtherClass() {
        return SrcAstTranslator.parse(
                createSrcFile("class OtherClass { String name; }", Path.of("OtherClass.java")), DEFAULT_PRINTER_CONFIG);
    }
}
