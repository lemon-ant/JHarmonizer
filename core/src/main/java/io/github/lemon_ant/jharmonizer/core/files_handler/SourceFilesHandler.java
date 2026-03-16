package io.github.lemon_ant.jharmonizer.core.files_handler;

import edu.umd.cs.findbugs.annotations.Nullable;
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
import lombok.Value;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class SourceFilesHandler {

    /**
     * Recursively resolves all `.java` files matching the given include and exclude glob patterns.
     * Supports mixed absolute and relative globs. Duplicates are removed.
     * For absolute globs, baseDir is extracted from the glob prefix. TODO rewrite
     * For relative globs, baseDir is current dir (".").
     * Excludes are applied as relative to each baseDir (assume excludes are relative; extend if needed for abs excludes).
     *
     * @param includeGlobs glob patterns to include (mixed abs/rel)
     * @param excludeGlobs glob patterns to exclude (relative to bases)
     * @return stream of unique absolute Java file paths (normalized)
     */
    // TODO Hide it and expose a new method readJavaFiles that combines findJavaFiles + readFile
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

    public static void overwrite(@NonNull Path path, @NonNull String fileContent) {
        try {
            Files.writeString(path, fileContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        log.trace("File content has been overwritten at {}", fileContent);
    }

    @NonNull
    // TODO Hide in readJavaFiles method
    public static SourceFilesHandler.SrcFile readFile(@NonNull Path file) {
        SrcFile srcFile;
        try {
            srcFile = new SrcFile(Files.readString(file, StandardCharsets.UTF_8), file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        log.trace("File content has been read from {}", file);
        return srcFile;
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    // TODO Hide in the Overwrite method
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

    @Value
    public static class SrcFile {
        @NonNull
        String srcCode;

        @NonNull
        Path path;

        @Override
        public boolean equals(@Nullable Object o) {
            if (!(o instanceof SrcFile that)) {
                return false;
            }
            return path.equals(that.path) && srcCode.equals(that.srcCode);
        }

        @Override
        public int hashCode() {
            int result = path.hashCode();
            result = 31 * result + srcCode.hashCode();
            return result;
        }
    }
}
