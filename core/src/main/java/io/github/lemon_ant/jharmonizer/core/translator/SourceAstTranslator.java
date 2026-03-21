package io.github.lemon_ant.jharmonizer.core.translator;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.getAllTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.getAllTypes;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.getRootTypes;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch.TimedResult;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Facade for parsing Java source files into Spoon AST models and serializing them back to source code.
 * Wraps {@link SpoonParser} and the Spoon source-printer, adding per-phase timing statistics.
 */
@Slf4j
@UtilityClass
public final class SourceAstTranslator {

    /**
     * Parses the source file.
     *
     * @param srcFile the source file to parse
     * @return the parsing result containing the Spoon model and parsing statistics
     */
    @SuppressWarnings("PMD.GuardLogStatement")
    @NonNull
    public static ParsingResult parse(@NonNull SrcFile srcFile) {
        log.debug("Parsing {}", srcFile.getPath());

        TimedResult<SpoonAstModel> parsingTimedResult = StopWatch.measure(() -> SpoonParser.parseJavaSrcFile(srcFile));

        SpoonAstModel spoonASTModel = parsingTimedResult.getResult();
        ParsingStatistic statistic = createParsingStatistic(srcFile.getSrcCode(), parsingTimedResult);
        return new ParsingResult(statistic, spoonASTModel);
    }

    /**
     * Serializes the sorted AST model back to source code.
     *
     * @param sortedSpoonAstModel the sorted Spoon AST model to serialize
     * @return the serialization result containing the source code and statistics
     */
    @SuppressWarnings("PMD.GuardLogStatement")
    @NonNull
    public static SerializationResult serialize(@NonNull SpoonAstModel sortedSpoonAstModel) {
        log.debug("Serializing {}", sortedSpoonAstModel.getPath());

        TimedResult<SerializedSourceWithSkippedTypeRanges> serializationTimedResult = StopWatch.measure(
                () -> sortedSpoonAstModel.getSerializedSrcCode().get());
        SerializedSourceWithSkippedTypeRanges serializedSourceWithSkippedTypeRanges =
                serializationTimedResult.getResult();

        return new SerializationResult(
                new SerializationStatistic(
                        serializedSourceWithSkippedTypeRanges
                                .getSerializedSrcCode()
                                .length(),
                        serializationTimedResult.getNanos()),
                serializedSourceWithSkippedTypeRanges);
    }

    @NonNull
    private static ParsingStatistic createParsingStatistic(
            String originalSrcCode, TimedResult<SpoonAstModel> parsingTimedResult) {
        SpoonAstModel spoonASTModel = parsingTimedResult.getResult();
        CtCompilationUnit compilationUnit = spoonASTModel.getCompilationUnit();

        // TODO It doesn't work String originalSrcCode = compilationUnit.getOriginalSourceCode();
        List<CtType<?>> rootTypes = getRootTypes(compilationUnit);
        List<CtType<?>> allDeclaredTypes = getAllTypes(compilationUnit);
        List<CtTypeMember> allTypesMembers = getAllTypeMembers(compilationUnit);

        return new ParsingStatistic(
                originalSrcCode.length(),
                allTypesMembers.size(),
                rootTypes.size(),
                allDeclaredTypes.size(),
                parsingTimedResult.getNanos());
    }
}
