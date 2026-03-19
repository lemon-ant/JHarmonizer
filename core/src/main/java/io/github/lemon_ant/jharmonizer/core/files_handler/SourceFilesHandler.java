package io.github.lemon_ant.jharmonizer.core.files_handler;

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
public class SourceFilesHandler {

    // TODO Hide it and expose a new method readJavaFiles that combines findJavaFiles + readFile
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
        return GlobPathFinder.findPaths(pathQuery);
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
        log.trace("File content has been overwritten at {}", fileContent);
    }

    // TODO Hide in readJavaFiles method
    /**
     * Reads a source file into a {@link SrcFile} value.
     *
     * @param file the source file to read
     * @return the loaded source file wrapper
     */
    @NonNull
    public static SrcFile readFile(@NonNull Path file) {
        SrcFile srcFile;
        try {
            srcFile = createSrcFile(Files.readString(file, StandardCharsets.UTF_8), file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        log.trace("File content has been read from {}", file);
        return srcFile;
    }

    /**
     * Builds a source-file wrapper from already loaded source text and its path.
     *
     * @param srcCode the source text
     * @param path the source file path
     * @return the source file wrapper
     */
    @NonNull
    public static SrcFile createSrcFile(@NonNull String srcCode, @NonNull Path path) {
        return new SrcFile(srcCode, path);
    }

    // TODO Hide in the Overwrite method
    /**
     * Renames the source file to its backup variant with a {@code .bak} suffix.
     *
     * @param sourceFile the source file to back up
     */
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public static void renameToBackup(@NonNull Path sourceFile) {
        if (!Files.exists(sourceFile) || !Files.isRegularFile(sourceFile)) {
            throw new UncheckedIOException(
                    new IOException("Source file does not exist or is not a valid file: " + sourceFile));
        }

        Path backupPath = sourceFile.resolveSibling(sourceFile.getFileName().toString() + ".bak");
        try {
            Files.move(sourceFile, backupPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        log.trace("File has been renamed to backup in {}", backupPath);
    }
}
