package io.github.lemon_ant.jharmonizer.core.translator;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonCompilationUnitUtilities.getAllTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonCompilationUnitUtilities.getAllTypes;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonCompilationUnitUtilities.getRootTypes;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler.FileContent;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch.TimedResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

@RequiredArgsConstructor
@Slf4j
public final class SourceAstTranslator {

    private static ParsingStatistic createParsingStatistic(
            String originalSourceCode, TimedResult<SpoonAstModel> parsingTimedResult) {
        SpoonAstModel spoonASTModel = parsingTimedResult.getResult();
        CtCompilationUnit originalCompilationUnit = spoonASTModel.getWorkingCompilationUnit();

        //  TODO It doesn't work  String originalSourceCode = originalCompilationUnit.getOriginalSourceCode();
        List<CtType<?>> rootTypes = getRootTypes(originalCompilationUnit);
        List<CtType<?>> allDeclaredTypes = getAllTypes(originalCompilationUnit);
        List<CtTypeMember> allTypesMembers = getAllTypeMembers(originalCompilationUnit);

        return new ParsingStatistic(
                originalSourceCode.length(),
                rootTypes.size(),
                allDeclaredTypes.size(),
                allTypesMembers.size(),
                parsingTimedResult.getNanos());
    }

    public ParsingResult parseSourceFile(FileContent sourceFileContent) {
        if (log.isInfoEnabled()) log.info("[Parser] Parsing {}!", sourceFileContent.getPath());

        TimedResult<SpoonAstModel> parsingTimedResult = StopWatch.measure(
                () -> SpoonParser.parseJavaSourceResource(sourceFileContent.getPath(), sourceFileContent.getContent()));

        SpoonAstModel spoonASTModel = parsingTimedResult.getResult();
        var statistic = createParsingStatistic(sourceFileContent.getContent(), parsingTimedResult);
        return new ParsingResult(spoonASTModel, statistic);
    }

    public SerializationResult serialize(SpoonAstModel sortedSpoonAstModel) {
        if (log.isInfoEnabled()) log.info("[Parser] Serializing!");

        TimedResult<String> serializationTimedResult = StopWatch.measure(
                () -> sortedSpoonAstModel.getSerializedSourceCode().get());
        String serializedSourceCode = serializationTimedResult.getResult();

        return new SerializationResult(
                serializedSourceCode,
                new SerializationStatistic(serializedSourceCode.length(), serializationTimedResult.getNanos()));
    }
}
