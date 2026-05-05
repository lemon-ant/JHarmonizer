// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.files_handler;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.lemon_ant.globpathfinder.GlobPathFinder;
import io.github.lemon_ant.globpathfinder.PathQuery;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class SrcFilesHandler {

    /**
     * Recursively resolves and reads all {@code .java} files matching the include and exclude globs
     * under the given base directory. GlobPathFinder scans the directory in parallel, so paths flow
     * directly through the pipeline without intermediate accumulation.
     *
     * @param baseDir the base directory to scan
     * @param includeGlobs the include globs to apply
     * @param excludeGlobs the exclude globs to apply
     * @return a stream of loaded source files
     */
    @NonNull
    public static Stream<SrcFile> readJavaFiles(
            @NonNull Path baseDir, @NonNull Collection<String> includeGlobs, @NonNull Collection<String> excludeGlobs) {
        return findJavaFiles(baseDir, includeGlobs, excludeGlobs).map(SrcFilesHandler::readFile);
    }

    /**
     * Recursively resolves all {@code .java} files that match the provided include and exclude globs.
     * Supports mixed absolute and relative globs and removes duplicates from the result.
     * Include globs whose effective base directory does not exist are silently skipped, rather than
     * causing {@link java.nio.file.NoSuchFileException} to propagate from the underlying file walker.
     *
     * @param baseDir the base directory to scan
     * @param includeGlobs the include globs to apply
     * @param excludeGlobs the exclude globs to apply
     * @return the matching Java file paths
     */
    @NonNull
    private static Stream<Path> findJavaFiles(
            @NonNull Path baseDir, @NonNull Collection<String> includeGlobs, @NonNull Collection<String> excludeGlobs) {
        // GlobPathFinder calls Files.find() eagerly on the effective base directory for each
        // include glob, and throws NoSuchFileException if that base does not exist. Pre-filter to
        // prevent this for globs whose effective base directory is absent (e.g. auto-derived
        // src/test/java patterns in modules that have no test sources).
        Set<String> reachableIncludes = includeGlobs.stream()
                .filter(glob -> effectiveBaseExists(baseDir, glob))
                .collect(Collectors.toUnmodifiableSet());
        if (!includeGlobs.isEmpty() && reachableIncludes.isEmpty()) {
            return Stream.empty();
        }
        PathQuery pathQuery = PathQuery.builder()
                .baseDir(baseDir)
                .includeGlobs(reachableIncludes)
                .excludeGlobs(excludeGlobs)
                .allowedExtensions(Set.of("java"))
                .build();
        return GlobPathFinder.findPaths(pathQuery).parallel();
    }

    private static boolean effectiveBaseExists(Path baseDir, String includeGlob) {
        // Absolute globs are resolved by GlobPathFinder independently; do not pre-filter them.
        if (Path.of(includeGlob).isAbsolute()) {
            return true;
        }
        // Walk the non-wildcard prefix to find the effective scan root that GlobPathFinder
        // would use. Stop at the first wildcard segment so we do not over-restrict.
        // Note: glob character ranges ([abc]) are also treated as wildcards here.
        Path effectiveBase = baseDir;
        for (String segment : includeGlob.split("[/\\\\]")) {
            if (segment.isEmpty() || segment.matches(".*[*?{\\[].*")) {
                break;
            }
            effectiveBase = effectiveBase.resolve(segment);
        }
        return Files.exists(effectiveBase);
    }

    /**
     * Performs the overwrite.
     * @param path the path to use
     * @param fileContent the file content
     */
    public static void overwrite(@NonNull Path path, @NonNull String fileContent) {
        try {
            Files.writeString(path, fileContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        log.trace("File content has been overwritten at {}", path);
    }

    /**
     * Reads a source file into a {@link SrcFile} value.
     *
     * @param file the source file to read
     * @return the loaded source file wrapper
     */
    @NonNull
    private static SrcFile readFile(@NonNull Path file) {
        SrcFile srcFile;
        try {
            srcFile = new SrcFile(Files.readString(file, StandardCharsets.UTF_8), file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        log.trace("File content has been read from {}", file);
        return srcFile;
    }

    // TODO Hide in the Overwrite method
    /**
     * Renames the source file to its backup variant with a {@code .bak} suffix.
     * If a backup already exists, it is replaced with the latest pre-overwrite source snapshot.
     *
     * @param srcFile the source file to back up
     */
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public static void renameToBackup(@NonNull Path srcFile) {
        if (!Files.exists(srcFile) || !Files.isRegularFile(srcFile)) {
            throw new UncheckedIOException(
                    new IOException("Source file does not exist or is not a valid file: " + srcFile));
        }

        Path backupPath = srcFile.resolveSibling(srcFile.getFileName().toString() + ".bak");
        try {
            Files.move(srcFile, backupPath, REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        log.trace("File has been renamed to backup in {}", backupPath);
    }
}
