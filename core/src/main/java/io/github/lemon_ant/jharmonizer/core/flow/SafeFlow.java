package io.github.lemon_ant.jharmonizer.core.flow;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.formatter.FormattingStatistic;
import io.github.lemon_ant.jharmonizer.core.sorter.SortingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingStatistic;
import io.github.lemon_ant.jharmonizer.core.translator.SerializationStatistic;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Runtime-protected {@link IFlow} wrapper that converts unexpected internal runtime failures
 * into {@link FlowProcessingStatus#ERROR} per-file results.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class SafeFlow implements IFlow {

    @NonNull
    private final IFlow delegate;

    /**
     * Wraps a flow with runtime-failure isolation for per-file processing.
     *
     * @param delegate the original flow implementation
     * @return an {@link IFlow} that returns {@code ERROR} results for unexpected runtime failures
     */
    @NonNull
    public static IFlow wrap(@NonNull IFlow delegate) {
        return new SafeFlow(delegate);
    }

    @Override
    @NonNull
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.GuardLogStatement"})
    public FlowProcessingResult processSrc(@NonNull SrcFile srcFile) {
        try {
            return delegate.processSrc(srcFile);
        } catch (RuntimeException exception) {
            log.warn(
                    "Unexpected internal processing error for file {}: {}",
                    srcFile.getPath(),
                    describeRuntimeFailure(exception));
            log.debug("Stack trace for processing error in file {}", srcFile.getPath(), exception);
            return FlowProcessingResult.builder()
                    .path(srcFile.getPath())
                    .relocations(List.of())
                    .diff("")
                    .parsingStatistic(new ParsingStatistic(
                            srcFile.getSrcCode().length(),
                            srcFile.getSrcCode().getBytes(StandardCharsets.UTF_8).length,
                            0,
                            0,
                            0,
                            0))
                    .sortingStatistic(new SortingStatistic(0))
                    .serializationStatistic(new SerializationStatistic(0, 0))
                    .formattingStatistic(new FormattingStatistic(0, 0))
                    .flowProcessingStatus(FlowProcessingStatus.ERROR)
                    .stopRequested(false)
                    .build();
        }
    }

    @NonNull
    private static String describeRuntimeFailure(@NonNull RuntimeException exception) {
        String exceptionType = exception.getClass().getSimpleName();
        String exceptionMessage = exception.getMessage();
        if (exceptionMessage == null || exceptionMessage.isBlank()) {
            return exceptionType;
        }
        return exceptionType + ": " + exceptionMessage;
    }
}
