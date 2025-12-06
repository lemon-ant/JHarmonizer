package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus.defineFlowProcessingStatus;
import static io.github.lemon_ant.jharmonizer.core.spoon.RelocationDetector.isRelocated;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler.FileContent;
import io.github.lemon_ant.jharmonizer.core.formatter.FormatingResult;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingResult;
import io.github.lemon_ant.jharmonizer.core.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationResult;
import io.github.lemon_ant.jharmonizer.core.translator.SourceAstTranslator;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class RestructureFlow implements IFlow {

    private final Formatter formatter;
    private final boolean backupsEnabled;
    private final Sorter sorter;

    @NonNull
    @Override
    public FlowProcessingResult processSource(@NonNull FileContent srcFileContent) {
        ParsingResult parsingResult = SourceAstTranslator.parseSourceFile(srcFileContent);
        SortingResult sortingResult = sorter.sort(parsingResult.getSpoonAstModel());
        SpoonAstModel sortedSpoonAstModel = sortingResult.getSortedSpoonAstModel();
        SerializationResult serializationResult = SourceAstTranslator.serialize(sortingResult.getSortedSpoonAstModel());
        FormatingResult formatingResult = formatter.formatSource(serializationResult.getSerializedSourceCode());

        boolean hasChanged = !srcFileContent.getContent().equals(formatingResult.getFormatedSourceCode());
        if (hasChanged) {
            if (backupsEnabled) {
                SourceFilesHandler.renameToBackup(srcFileContent.getPath());
            }
            SourceFilesHandler.overwrite(srcFileContent.getPath(), formatingResult.getFormatedSourceCode());
        }

        return FlowProcessingResult.builder()
                .path(srcFileContent.getPath())
                .relocations(null)
                .diff(null)
                .parsingStatistic(parsingResult.getParsingStatistic())
                .sortingStatistic(sortingResult.getSortingStatistic())
                .serializationStatistic(serializationResult.getSerializationStatistic())
                .formatingStatistic(formatingResult.getFormatingStatistic())
                .flowProcessingStatus(defineFlowProcessingStatus(
                        isRelocated(
                                sortedSpoonAstModel.getOriginalElements2OrderIndices(),
                                sortedSpoonAstModel.getCompilationUnit()),
                        hasChanged,
                        false))
                .build();
    }
}
