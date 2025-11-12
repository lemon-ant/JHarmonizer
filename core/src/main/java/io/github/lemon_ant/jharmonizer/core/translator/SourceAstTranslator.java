package io.github.lemon_ant.jharmonizer.core.translator;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonUtilities.getAllTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonUtilities.getAllTypes;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonUtilities.getRootTypes;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler.FileContent;
import io.github.lemon_ant.jharmonizer.core.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.spoon.SpoonParser;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch;
import io.github.lemon_ant.jharmonizer.core.utilities.StopWatch.TimedResult;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

@Slf4j
@UtilityClass
public final class SourceAstTranslator {

    @SuppressWarnings("PMD.GuardLogStatement")
    public static ParsingResult parseSourceFile(FileContent sourceFileContent) {
        log.debug("Parsing {}", sourceFileContent.getPath());

        TimedResult<SpoonAstModel> parsingTimedResult = StopWatch.measure(
                () -> SpoonParser.parseJavaSourceResource(sourceFileContent.getPath(), sourceFileContent.getContent()));

        SpoonAstModel spoonASTModel = parsingTimedResult.getResult();
        ParsingStatistic statistic = createParsingStatistic(sourceFileContent.getContent(), parsingTimedResult);
        return new ParsingResult(statistic, spoonASTModel);
    }

    public static SerializationResult serialize(SpoonAstModel sortedSpoonAstModel) {
        log.debug("Serializing");

        TimedResult<String> serializationTimedResult = StopWatch.measure(
                () -> sortedSpoonAstModel.getSerializedSourceCode().get());
        String serializedSourceCode = serializationTimedResult.getResult();

        return new SerializationResult(
                new SerializationStatistic(serializedSourceCode.length(), serializationTimedResult.getNanos()),
                serializedSourceCode);
    }

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
