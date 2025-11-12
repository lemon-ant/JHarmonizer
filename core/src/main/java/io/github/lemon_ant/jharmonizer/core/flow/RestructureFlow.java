package io.github.lemon_ant.jharmonizer.core.flow;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler.FileContent;
import io.github.lemon_ant.jharmonizer.core.formatter.FormatingResult;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingResult;
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
    private final boolean isMakingBackups;
    private final Sorter sorter;

    @NonNull
    @Override
    public FlowProcessingResult processSource(@NonNull FileContent srcFileContent) {
        ParsingResult parsingResult = SourceAstTranslator.parseSourceFile(srcFileContent);
        SortingResult sortingResult = sorter.sort(parsingResult.getSpoonAstModel());
        SerializationResult serializationResult = SourceAstTranslator.serialize(sortingResult.getSortedSpoonAstModel());
        FormatingResult formatingResult = formatter.formatSource(serializationResult.getSerializedSourceCode());

        boolean hasChanged = !srcFileContent.getContent().equals(formatingResult.getFormatedSourceCode());
        if (hasChanged) {
            if (isMakingBackups) {
                SourceFilesHandler.renameToBackup(srcFileContent.getPath());
            }
            SourceFilesHandler.overwrite(srcFileContent.getPath(), formatingResult.getFormatedSourceCode());
        }

        return new FlowProcessingResult(
                srcFileContent.getPath(),
                null,
                null,
                parsingResult.getParsingStatistic(),
                sortingResult.getSortingStatistic(),
                serializationResult.getSerializationStatistic(),
                formatingResult.getFormatingStatistic());
    }
}
