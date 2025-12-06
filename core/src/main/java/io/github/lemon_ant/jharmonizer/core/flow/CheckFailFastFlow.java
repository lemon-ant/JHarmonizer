package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.spoon.RelocationDetector.findRelocations;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler.FileContent;
import io.github.lemon_ant.jharmonizer.core.formatter.FormatingResult;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingResult;
import io.github.lemon_ant.jharmonizer.core.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationResult;
import io.github.lemon_ant.jharmonizer.core.translator.SourceAstTranslator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.declaration.CtElement;

@Slf4j
@AllArgsConstructor
public class CheckFailFastFlow implements IFlow {

    private final Formatter formatter;
    private final Sorter sorter;

    @Override
    public @NonNull FlowProcessingResult processSource(@NonNull FileContent srcFileContent) {
        // Parse
        ParsingResult parsingResult = SourceAstTranslator.parseSourceFile(srcFileContent);

        // Sort (Fail Fast)
        SortingResult sortingResult = sorter.sort(parsingResult.getSpoonAstModel());
        SpoonAstModel sortedSpoonAstModel = sortingResult.getSortedSpoonAstModel();
        List<Pair<CtElement, Integer>> elementRelocations = findRelocations(
                sortedSpoonAstModel.getOriginalElements2OrderIndices(), sortedSpoonAstModel.getCompilationUnit());
        if (!elementRelocations.isEmpty()) {
            throw new NotOrderedException(srcFileContent.getPath(), elementRelocations);
        }

        // Serialize
        SerializationResult serializationResult = SourceAstTranslator.serialize(sortedSpoonAstModel);

        // Format (Fail Fast)
        FormatingResult formatingResult = formatter.formatSource(serializationResult.getSerializedSourceCode());
        if (!srcFileContent.getContent().equals(formatingResult.getFormatedSourceCode())) {
            String srcDiff = computeDiff(srcFileContent.getContent(), formatingResult.getFormatedSourceCode());
            throw new NotFormattedException(srcFileContent.getPath(), srcDiff);
        }

        return FlowProcessingResult.builder()
                .path(srcFileContent.getPath())
                .relocations(null)
                .diff("")
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(sortingResult.getSortingStatistic())
                .serializationStatistic(serializationResult.getSerializationStatistic())
                .formatingStatistic(formatingResult.getFormatingStatistic())
                .flowProcessingStatus(defineFlowProcessingStatus(false, false, true))
                .build();
    }
}
