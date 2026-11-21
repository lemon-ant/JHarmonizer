// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonTypeTreeUtils.streamAllTypes;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonPrintingInput;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinter;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch.TimedResult;
import java.nio.charset.StandardCharsets;
import java.util.IntSummaryStatistics;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * Facade for parsing Java source files into Spoon AST models and serializing them back to source code.
 * Wraps {@link SpoonParser} and the Spoon source-printer, adding per-phase timing statistics.
 */
@Slf4j
@UtilityClass
public final class SrcAstTranslator {

    @NonNull
    private static final TypeFilter<CtTypeMember> TYPE_MEMBER_FILTER = new TypeFilter<>(CtTypeMember.class);

    /**
     * Parses the source file.
     *
     * @param srcFile the source file to parse
     * @return the parsing result containing the Spoon model and parsing statistics
     */
    @SuppressWarnings("PMD.GuardLogStatement")
    @NonNull
    public static ParsingResult parse(@NonNull SrcFile srcFile) {
        log.trace("Parsing {}", srcFile.getPath());

        TimedResult<SpoonAstModel> parsingTimedResult = StopWatch.measure(() -> SpoonParser.parseJavaSrcFile(srcFile));

        SpoonAstModel spoonASTModel = parsingTimedResult.getResult();
        ParsingStatistic statistic = createParsingStatistic(srcFile.getSrcCode(), parsingTimedResult);
        return new ParsingResult(statistic, spoonASTModel);
    }

    /**
     * Serializes declarations in AST order and applies selected annotation orders.
     *
     * @param printingInput shared AST and source context with the annotation orders to print
     * @param printerConfig the source printer settings for this invocation
     * @return the serialization result containing the source code and statistics
     */
    @SuppressWarnings("PMD.GuardLogStatement")
    @NonNull
    public static SerializationResult serialize(
            @NonNull SpoonPrintingInput printingInput, @NonNull PrinterConfig printerConfig) {
        log.trace("Serializing {}", printingInput.getPath());

        TimedResult<SerializedSrcWithSkippedTypeRanges> serializationTimedResult =
                StopWatch.measure(() -> SpoonSrcPrinter.serializeCompilationUnit(printingInput, printerConfig));
        SerializedSrcWithSkippedTypeRanges serializedSrcWithSkippedTypeRanges = serializationTimedResult.getResult();

        return new SerializationResult(
                new SerializationStatistic(
                        serializedSrcWithSkippedTypeRanges
                                .getSerializedSrcCode()
                                .length(),
                        serializationTimedResult.getNanos()),
                serializedSrcWithSkippedTypeRanges);
    }

    @NonNull
    private static ParsingStatistic createParsingStatistic(
            String originalSrcCode, TimedResult<SpoonAstModel> parsingTimedResult) {
        SpoonAstModel spoonASTModel = parsingTimedResult.getResult();
        CtCompilationUnit compilationUnit = spoonASTModel.getCompilationUnit();

        // TODO It doesn't work String originalSrcCode = compilationUnit.getOriginalSourceCode();
        // Summarize the existing subtree counts in one pass without aggregate type/member lists.
        // Spoon includes each type itself and implicit members; nested subtrees contribute to each enclosing count.
        IntSummaryStatistics memberCountsByType = streamAllTypes(compilationUnit)
                .mapToInt(type -> type.getElements(TYPE_MEMBER_FILTER).size())
                .summaryStatistics();

        return new ParsingStatistic(
                originalSrcCode.length(),
                originalSrcCode.getBytes(StandardCharsets.UTF_8).length,
                Math.toIntExact(memberCountsByType.getSum()),
                compilationUnit.getDeclaredTypes().size(),
                Math.toIntExact(memberCountsByType.getCount()),
                parsingTimedResult.getNanos());
    }
}
