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
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class SrcFilesHandler {

    /**
     * Recursively resolves all {@code .java} files that match the provided include and exclude globs
     * across all given base directories. Removes duplicate paths that appear under multiple base dirs.
     *
     * @param baseDirs the base directories to scan
     * @param includeGlobs the include globs to apply
     * @param excludeGlobs the exclude globs to apply
     * @return the matching Java file paths, deduplicated
     */
    @NonNull
    public static Stream<Path> findJavaFiles(
            @NonNull Collection<Path> baseDirs,
            @NonNull Collection<String> includeGlobs,
            @NonNull Collection<String> excludeGlobs) {
        return baseDirs.stream()
                .flatMap(baseDir -> findJavaFiles(baseDir, includeGlobs, excludeGlobs))
                .distinct();
    }

    /**
     * Recursively resolves all {@code .java} files that match the provided include and exclude globs.
     * Supports mixed absolute and relative globs and removes duplicates from the result.
     *
     * @param baseDir the base directory to scan
     * @param includeGlobs the include globs to apply
     * @param excludeGlobs the exclude globs to apply
     * @return the matching Java file paths
     */
    @NonNull
    public static Stream<Path> findJavaFiles(
            @NonNull Path baseDir, @NonNull Collection<String> includeGlobs, @NonNull Collection<String> excludeGlobs) {
        PathQuery pathQuery = PathQuery.builder()
                .baseDir(baseDir)
                .includeGlobs(includeGlobs)
                .excludeGlobs(excludeGlobs)
                .allowedExtensions(Set.of("java"))
                .build();
        return GlobPathFinder.findPaths(pathQuery).parallel();
    }

    /**
     * Recursively resolves and reads all {@code .java} files matching the include and exclude globs
     * across all given base directories. Removes duplicate paths before reading.
     *
     * @param baseDirs the base directories to scan
     * @param includeGlobs the include globs to apply
     * @param excludeGlobs the exclude globs to apply
     * @return a stream of loaded source files
     */
    @NonNull
    public static Stream<SrcFile> readJavaFiles(
            @NonNull Collection<Path> baseDirs,
            @NonNull Collection<String> includeGlobs,
            @NonNull Collection<String> excludeGlobs) {
        return findJavaFiles(baseDirs, includeGlobs, excludeGlobs).map(SrcFilesHandler::readFile);
    }

    /**
     * Recursively resolves and reads all {@code .java} files matching the include and exclude globs.
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
