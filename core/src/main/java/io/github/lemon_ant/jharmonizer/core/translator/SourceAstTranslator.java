package io.github.lemon_ant.jharmonizer.core.translator;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.getAllTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.getAllTypes;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.getRootTypes;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler.SrcFile;
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
     * @param sourceSrcFile the source src file
     * @return the source file
     */
    @SuppressWarnings("PMD.GuardLogStatement")
    public static ParsingResult parseSourceFile(SrcFile sourceSrcFile) {
        log.debug("Parsing {}", sourceSrcFile.getPath());

        TimedResult<SpoonAstModel> parsingTimedResult = StopWatch.measure(
                () -> SpoonParser.parseJavaSourceResource(sourceSrcFile.getPath(), sourceSrcFile.getSrcCode()));

        SpoonAstModel spoonASTModel = parsingTimedResult.getResult();
        ParsingStatistic statistic = createParsingStatistic(sourceSrcFile.getSrcCode(), parsingTimedResult);
        return new ParsingResult(statistic, spoonASTModel);
    }

    /**
     * Serializes the sorted AST model back to source code.
     *
     * @param sortedSpoonAstModel the sorted Spoon AST model to serialize
     * @return the serialization result containing the source code and statistics
     */
    @SuppressWarnings("PMD.GuardLogStatement")
    public static SerializationResult serialize(SpoonAstModel sortedSpoonAstModel) {
        log.debug("Serializing {}", sortedSpoonAstModel.getPath());

        TimedResult<SerializedSourceSnapshot> serializationTimedResult = StopWatch.measure(
                () -> sortedSpoonAstModel.getSerializedSourceCode().get());
        SerializedSourceSnapshot serializedSourceSnapshot = serializationTimedResult.getResult();
        String serializedSourceCode = serializedSourceSnapshot.getSourceCode();

        return new SerializationResult(
                new SerializationStatistic(serializedSourceCode.length(), serializationTimedResult.getNanos()),
                serializedSourceCode,
                serializedSourceSnapshot.getFormattingExclusionRanges());
    }

    @NonNull
    private static ParsingStatistic createParsingStatistic(
            String originalSourceCode, TimedResult<SpoonAstModel> parsingTimedResult) {
        SpoonAstModel spoonASTModel = parsingTimedResult.getResult();
        CtCompilationUnit compilationUnit = spoonASTModel.getCompilationUnit();

        // TODO It doesn't work String originalSourceCode = compilationUnit.getOriginalSourceCode();
        List<CtType<?>> rootTypes = getRootTypes(compilationUnit);
        List<CtType<?>> allDeclaredTypes = getAllTypes(compilationUnit);
        List<CtTypeMember> allTypesMembers = getAllTypeMembers(compilationUnit);

        return new ParsingStatistic(
                originalSourceCode.length(),
                allTypesMembers.size(),
                rootTypes.size(),
                allDeclaredTypes.size(),
                parsingTimedResult.getNanos());
    }
}
