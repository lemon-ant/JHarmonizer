package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.diff.DiffReporter.computeDiff;
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
public class CheckAllFlow implements IFlow {

    private final Formatter formatter;
    private final Sorter sorter;

    @NonNull
    @Override
    public FlowProcessingResult processSource(@NonNull FileContent srcFileContent) {
        ParsingResult parsingResult = SourceAstTranslator.parseSourceFile(srcFileContent);
        SortingResult sortingResult = sorter.sort(parsingResult.getSpoonAstModel());
        SpoonAstModel sortedSpoonAstModel = sortingResult.getSortedSpoonAstModel();
        SerializationResult serializationResult = SourceAstTranslator.serialize(sortedSpoonAstModel);
        FormatingResult formatingResult = formatter.formatSource(serializationResult.getSerializedSourceCode());

        boolean hasChanges = !srcFileContent.getContent().equals(serializationResult.getSerializedSourceCode());
        List<Pair<CtElement, Integer>> elementRelocations;
        String srcDiff;
        if (hasChanges) {
            elementRelocations = findRelocations(
                    sortedSpoonAstModel.getOriginalElements2OrderIndices(), sortedSpoonAstModel.getCompilationUnit());
            srcDiff = computeDiff(srcFileContent.getContent(), formatingResult.getFormatedSourceCode());
        } else {
            elementRelocations = List.of();
            srcDiff = "";
        }

        return FlowProcessingResult.builder()
                .path(srcFileContent.getPath())
                .relocations(elementRelocations)
                .diff(srcDiff)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(sortingResult.getSortingStatistic())
                .serializationStatistic(serializationResult.getSerializationStatistic())
                .formatingStatistic(formatingResult.getFormatingStatistic())
                .build();
    }
}
