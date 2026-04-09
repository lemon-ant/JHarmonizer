package io.github.lemon_ant.globpathfinder;

import static io.github.lemon_ant.globpathfinder.FileMatchingUtils.computeBaseToIncludeMatchers;
import static io.github.lemon_ant.globpathfinder.FileMatchingUtils.partitionAbsoluteAndRelative;
import static io.github.lemon_ant.globpathfinder.StringUtils.processNormalizedStrings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.helpers.MessageFormatter;

/**
 * GlobPathFinder — service that traverses the file system and applies include/exclude rules.
 *
 * <p>Behavior overview (data comes from {@link PathQuery}):</p>
 * <ul>
 *   <li><b>Base directory</b>: {@code pathQuery.baseDir} is normalized to an absolute, normalized path before traversal.</li>
 *   <li><b>Include globs</b>: patterns are grouped by extracted base via {@code computeBaseToPattern}. For a base path,
 *       an empty matcher set means “match all under that base”.</li>
 *   <li><b>Extensions</b>: case-insensitive filter built from {@code allowedExtensions}. An empty set disables the filter.</li>
 *   <li><b>Excludes</b>: relative/absolute patterns compiled as {@code PathMatcher("glob:...")}.</li>
 *   <li><b>Files/directories</b>: {@code onlyFiles=true} keeps regular files only; otherwise both files and directories may appear.</li>
 *   <li><b>Depth/options</b>: {@code maxDepth} and {@code getVisitOptions()} are passed to {@link java.nio.file.Files#find}.</li>
 *   <li><b>Parallelism</b>: different base directories are scanned in parallel; downstream is {@code unordered()}.</li>
 *   <li><b>Uniqueness</b>: resulting paths are deduplicated; the stream yields unique entries ({@code distinct()}).</li>
 *   <li><b>Stream safety</b>: the inner stream from {@code Files.find(...)} is closed via {@code onClose} when the outer stream is closed.</li>
 * </ul>
 */
@Slf4j
@UtilityClass
public class GlobPathFinder {

    public static final String FAILED_TO_START_SCANNING_BASE =
            "Failed to start scanning base '{}'. Skipping this base.";

    /**
     * Find paths according to the provided {@link PathQuery}.
     *
     * @param pathQuery configuration (base, include/exclude, extensions, depth, onlyFiles)
     * @return a stream of absolute, normalized and <b>unique</b> paths that satisfy the filters;
     *         the caller is responsible for closing the returned stream
     * @throws UncheckedIOException on IO errors during traversal
     */
    @NonNull
    public static Stream<Path> findPaths(@NonNull PathQuery pathQuery) {
        // DEBUG: entrypoint info
        log.debug("findPaths: starting with query {}", pathQuery);

        // 1) Normalize inputs and precompute matchers/sets (no I/O here).
        Path normalizedBaseDir = pathQuery.getBaseDir().toAbsolutePath().normalize();

        // Group include globs by extracted base; empty matcher set for a base denotes MATCH_ALL under that base.
        Map<Path, Set<PathMatcher>> baseToIncludeMatchers =
                computeBaseToIncludeMatchers(normalizedBaseDir, pathQuery.getIncludeGlobs());

        // ===== Build processing pipeline BEFORE any stream is created =====

        // Extensions (case-insensitive); if empty, there will be NO extension step in the pipeline.
        Set<String> normalizedExtensions = buildNormalizedExtensions(pathQuery);
        boolean hasAllowedExtensions = !normalizedExtensions.isEmpty();

        // Excludes: split into absolute vs relative; if a set is empty, there will be NO respective step.
        Pair<Set<PathMatcher>, Set<PathMatcher>> absoluteAndRelativeExcludeMatchers = compileExcludeMatchers(pathQuery);
        Set<PathMatcher> absoluteExcludeMatchers = absoluteAndRelativeExcludeMatchers.getLeft();
        Set<PathMatcher> relativeExcludeMatchers = absoluteAndRelativeExcludeMatchers.getRight();

        BiPredicate<Path, BasicFileAttributes> fileTypeFilter = buildFileTypeFilter(pathQuery.isOnlyFiles());

        // 2) Compose global pipeline once (base-agnostic).
        Function<Stream<Path>, Stream<Path>> globalPipeline =
                buildGlobalPipeline(normalizedExtensions, hasAllowedExtensions, absoluteExcludeMatchers);

        // 3) Compose per-base pipeline factory (adds relative-phase only when needed for that base).
        Function<Entry<Path, Set<PathMatcher>>, Function<Stream<Path>, Stream<Path>>> perBasePipelineFactory =
                buildPerBasePipelineFactory(relativeExcludeMatchers);

        // 4) Start traversal: each base is scanned in parallel; I/O is isolated inside scanBase(...) with shielding.
        Stream<Path> resultPathStream = baseToIncludeMatchers.entrySet().parallelStream()
                .flatMap(entry -> scanBaseDir(entry, pathQuery, globalPipeline, perBasePipelineFactory, fileTypeFilter))
                // Keep downstream parallel-friendly; order is irrelevant.
                .unordered()
                .distinct();
        if (log.isDebugEnabled()) {
            // DEBUG: final emission (after all filters)
            resultPathStream = resultPathStream.peek(path -> {
                log.debug("Emitting {}", path);
            });
        }
        return resultPathStream;
    }

    /**
     * Files.find(...) predicate: either regular files only or pass-through. This is not a stream operator.
     */
    private static BiPredicate<Path, BasicFileAttributes> buildFileTypeFilter(boolean onlyFiles) {
        return onlyFiles ? (path, attrs) -> attrs.isRegularFile() : (path, attrs) -> true;
    }

    /**
     * Compose the global (base-agnostic) pipeline that:
     * - optionally logs raw discoveries,
     * - optionally filters by extension,
     * - optionally filters by absolute excludes.
     */
    private static Function<Stream<Path>, Stream<Path>> buildGlobalPipeline(
            Set<String> normalizedExtensions, boolean hasAllowedExtensions, Set<PathMatcher> absoluteExcludeMatchers) {
        // We compose steps only when they are needed. No "path -> true" fallbacks.
        Function<Stream<Path>, Stream<Path>> globalPipeline = Function.identity();

        // Debug peek on the raw discoveries
        if (log.isTraceEnabled()) {
            globalPipeline = globalPipeline.andThen(pathStream -> pathStream.peek(path -> log.trace("Found {}", path)));
        }

        // Extensions filter
        if (hasAllowedExtensions) {
            globalPipeline = globalPipeline.andThen(pathStream -> pathStream.filter(path -> {
                // Compute extension lazily only when this step exists.
                String extension = StringUtils.lowerCase(FilenameUtils.getExtension(path.toString()));
                return normalizedExtensions.contains(extension);
            }));
            if (log.isTraceEnabled()) {
                globalPipeline = globalPipeline.andThen(
                        pathStream -> pathStream.peek(path -> log.trace("Passed extension filter {}", path)));
            }
        }

        // Absolute excludes
        if (!absoluteExcludeMatchers.isEmpty()) {
            globalPipeline = globalPipeline.andThen(pathStream ->
                    pathStream.filter(path -> absoluteExcludeMatchers.stream().noneMatch(m -> m.matches(path))));
            if (log.isTraceEnabled()) {
                globalPipeline = globalPipeline.andThen(
                        pathStream -> pathStream.peek(path -> log.trace("Passed exclude absolute filter {}", path)));
            }
        }

        return globalPipeline;
    }

    /**
     * Build lower-cased extension set; empty set disables the extension filter.
     */
    private static Set<String> buildNormalizedExtensions(PathQuery pathQuery) {
        return processNormalizedStrings(
                pathQuery.getAllowedExtensions(), extension -> extension.toLowerCase(Locale.ROOT));
    }

    /**
     * Factory that builds a per-base pipeline. It adds a “relative phase”
     * (relativize → per-base include → relative excludes) only if needed for that specific base.
     */
    private static Function<Entry<Path, Set<PathMatcher>>, Function<Stream<Path>, Stream<Path>>>
            buildPerBasePipelineFactory(Set<PathMatcher> relativeExcludeMatchers) {
        return entry -> {
            Path basePath = entry.getKey();
            Set<PathMatcher> includeMatchersForBase = entry.getValue();
            boolean hasIncludesForBase = !includeMatchersForBase.isEmpty();
            boolean hasRelativeExcludes = !relativeExcludeMatchers.isEmpty();

            if (!(hasIncludesForBase || hasRelativeExcludes)) {
                return Function.identity();
            }

            // 1) relativize
            Function<Stream<Path>, Stream<Path>> perBaseDirPipeline =
                    pathStream -> pathStream.map(basePath::relativize);

            // 2) include (only if present for this base)
            if (hasIncludesForBase) {
                perBaseDirPipeline = perBaseDirPipeline.andThen(pathStream -> pathStream.filter(
                        relPath -> FileMatchingUtils.isMatchedToPatterns(relPath, includeMatchersForBase)));
                if (log.isTraceEnabled()) {
                    perBaseDirPipeline = perBaseDirPipeline.andThen(
                            pathStream -> pathStream.peek(path -> log.trace("Passed include filter {}", path)));
                }
            }

            // 3) relative excludes (only if configured at all)
            if (hasRelativeExcludes) {
                perBaseDirPipeline = perBaseDirPipeline.andThen(pathStream -> pathStream.filter(
                        relPath -> relativeExcludeMatchers.stream().noneMatch(m -> m.matches(relPath))));
                if (log.isTraceEnabled()) {
                    perBaseDirPipeline = perBaseDirPipeline.andThen(pathStream ->
                            pathStream.peek(path -> log.trace("Passed exclude relative filter {}", path)));
                }
            }

            // 4) return to absolute, normalized
            perBaseDirPipeline = perBaseDirPipeline.andThen(pathStream -> pathStream.map(
                    relPath -> basePath.resolve(relPath).toAbsolutePath().normalize()));
            return perBaseDirPipeline;
        };
    }

    /**
     * Split excludes to absolute/relative and compile to PathMatcher(glob:...).
     */
    private static Pair<Set<PathMatcher>, Set<PathMatcher>> compileExcludeMatchers(PathQuery pathQuery) {
        Pair<List<String>, List<String>> absoluteAndRelativeExcludes =
                partitionAbsoluteAndRelative(pathQuery.getExcludeGlobs());

        // Compile exclude globs to PathMatcher (glob:...) and evaluate against ABSOLUTE paths.
        Set<PathMatcher> absoluteExcludeMatchers =
                processNormalizedStrings(absoluteAndRelativeExcludes.getLeft(), pattern -> FileSystems.getDefault()
                        .getPathMatcher("glob:" + pattern));

        // Compile exclude globs to PathMatcher (glob:...) and evaluate against RELATIVE paths.
        Set<PathMatcher> relativeExcludeMatchers =
                processNormalizedStrings(absoluteAndRelativeExcludes.getRight(), pattern -> FileSystems.getDefault()
                        .getPathMatcher("glob:" + pattern));

        return Pair.of(absoluteExcludeMatchers, relativeExcludeMatchers);
    }

    /**
     * Base scan that:
     * <ul>
     *   <li>starts {@code Files.find(...)} with configured depth and options,</li>
     *   <li><b>conditionally</b> wraps with {@code IoTolerantPathStream}:
     *       shielded (log+swallow) when {@code failFastOnError == false};
     *       pass-through (rethrow) when {@code failFastOnError == true},</li>
     *   <li>applies global + per-base pipelines,</li>
     *   <li>ensures the inner stream is closed when the resulting stream is closed.</li>
     * </ul>
     */
    private static Stream<Path> scanBaseDir(
            Entry<Path, Set<PathMatcher>> baseEntry,
            PathQuery pathQuery,
            Function<Stream<Path>, Stream<Path>> globalPipeline,
            Function<Entry<Path, Set<PathMatcher>>, Function<Stream<Path>, Stream<Path>>> perBasePipelineFactory,
            BiPredicate<Path, BasicFileAttributes> fileTypeFilter) {
        Path basePath = baseEntry.getKey();
        try {
            Stream<Path> foundPaths = Files.find(
                    basePath,
                    pathQuery.getMaxDepth(),
                    fileTypeFilter,
                    pathQuery.getVisitOptions().toArray(new FileVisitOption[0]));

            // If fail-fast is enabled, do NOT shield: let UncheckedIOException bubble up.
            // Otherwise wrap to log+swallow and cut only the current branch.
            Stream<Path> shieldedPaths =
                    pathQuery.isFailFastOnError() ? foundPaths : IoTolerantPathStream.wrap(foundPaths, basePath);

            Function<Stream<Path>, Stream<Path>> perBasePipeline = perBasePipelineFactory.apply(baseEntry);

            return perBasePipeline.apply(globalPipeline.apply(shieldedPaths)).onClose(shieldedPaths::close);
        } catch (IOException startFailure) {
            // Early failure when creating the stream (not during iteration).
            if (pathQuery.isFailFastOnError()) {
                throw new UncheckedIOException(
                        MessageFormatter.format(FAILED_TO_START_SCANNING_BASE, basePath)
                                .getMessage(),
                        startFailure);
            }
            log.warn(FAILED_TO_START_SCANNING_BASE, basePath, startFailure);
            return Stream.empty();
        }
    }
}
