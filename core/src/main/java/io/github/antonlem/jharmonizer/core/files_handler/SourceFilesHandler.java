package io.github.antonlem.jharmonizer.core.files_handler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class SourceFilesHandler {
    private final boolean isMakingBackup;

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public final void backup(@NonNull Path sourceFile) throws IOException {
        if (!Files.exists(sourceFile) || !Files.isRegularFile(sourceFile)) {
            throw new IOException("Source file does not exist or is not a valid file: " + sourceFile);
        }

        Path backupPath = sourceFile.resolveSibling(sourceFile.getFileName().toString() + ".bak");
        Files.move(sourceFile, backupPath);

        if (log.isInfoEnabled()) {
            log.info("File renamed (backup) to {}", backupPath);
        }
    }

    /**
     * Recursively resolves all `.java` files matching the given include and exclude glob patterns.
     * Supports mixed absolute and relative globs. Duplicates are removed.
     * For absolute globs, baseDir is extracted from the glob prefix.
     * For relative globs, baseDir is current dir (".").
     * Excludes are applied as relative to each baseDir (assume excludes are relative; extend if needed for abs excludes).
     *
     * @param includeGlobs glob patterns to include (mixed abs/rel)
     * @param excludeGlobs glob patterns to exclude (relative to bases)
     * @return stream of unique absolute Java file paths (normalized)
     */


        public static Stream<Path> resolveJavaFiles(Collection<String> includeGlobs, Collection<String> excludeGlobs) {
            FileSystem fs = FileSystems.getDefault();
            Path cwd = Path.of(".").toAbsolutePath().normalize();

            // Подготовка exclude matchers (assume relative; for abs — treat as global)
            List<PathMatcher> globalExcludeMatchers = excludeGlobs.stream()
                .filter(SourceFilesHandler::isAbsoluteGlob)
                .map(glob -> fs.getPathMatcher("glob:" + glob))
                .toList();
            List<PathMatcher> relativeExcludeMatchers = excludeGlobs.stream()
                .filter(glob -> !isAbsoluteGlob(glob))
                .map(glob -> fs.getPathMatcher("glob:" + glob))
                .toList();

            // Группировка include по baseDir
            Map<Path, List<PathMatcher>> groupedMatchers = includeGlobs.stream()
                .collect(Collectors.groupingBy(
                    glob -> resolveBaseDir(glob, cwd),
                    Collectors.mapping(
                        glob -> createRelativeMatcher(glob, fs, cwd),
                        Collectors.toList()
                    )
                ));

            // Параллельный обход
            return groupedMatchers.entrySet().parallelStream()
                .flatMap(entry -> {
                    Path baseDir = entry.getKey();
                    List<PathMatcher> includeMatchers = entry.getValue();

                    if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
                        System.err.println("Warning: Skipping non-existent baseDir: " + baseDir);
                        return Stream.empty();
                    }

                    try (Stream<Path> stream = Files.walk(baseDir, 100, FileVisitOption.FOLLOW_LINKS)) {
                        List<Path> paths = stream
                            .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java"))
                            .filter(path -> {
                                Path relative = baseDir.relativize(path);
                                return includeMatchers.stream().anyMatch(m -> m.matches(relative)) &&
                                       relativeExcludeMatchers.stream().noneMatch(m -> m.matches(relative)) &&
                                       globalExcludeMatchers.stream().noneMatch(m -> m.matches(path));
                            })
                            .toList(); // Collect inside to avoid close
                        return paths.stream();
                    } catch (IOException e) {
                        System.err.println("Warning: Failed to walk baseDir '" + baseDir + "': " + e.getMessage());
                        return Stream.empty();
                    }
                })
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .distinct();
        }

        // Helper methods as before
        private static boolean isAbsoluteGlob(String glob) {
            return glob.startsWith("/") || (glob.length() > 1 && Character.isLetter(glob.charAt(0)) && glob.charAt(1) == ':');
        }

        private static Path extractBaseDirFromGlob(String glob) {
            String normalizedGlob = glob.replace('\\', '/');
            int firstWildcard = Math.min(
                normalizedGlob.indexOf('*') != -1 ? normalizedGlob.indexOf('*') : Integer.MAX_VALUE,
                normalizedGlob.indexOf('?') != -1 ? normalizedGlob.indexOf('?') : Integer.MAX_VALUE
            );
            if (firstWildcard == Integer.MAX_VALUE) {
                Path path = Path.of(normalizedGlob).normalize().toAbsolutePath();
                return Files.isDirectory(path) ? path : path.getParent();
            }

            int lastSlashBeforeWildcard = normalizedGlob.lastIndexOf('/', firstWildcard - 1);
            String prefix = lastSlashBeforeWildcard >= 0 ? normalizedGlob.substring(0, lastSlashBeforeWildcard) : "/";
            return Path.of(prefix).normalize().toAbsolutePath();
        }

        private static Path resolveBaseDir(String glob, Path cwd) {
            if (isAbsoluteGlob(glob)) {
                Path base = extractBaseDirFromGlob(glob);
                return base.normalize().toAbsolutePath();
            }
            return cwd;
        }

        private static PathMatcher createRelativeMatcher(String glob, FileSystem fs, Path cwd) {
            if (isAbsoluteGlob(glob)) {
                Path baseDir = extractBaseDirFromGlob(glob);
                String baseStr = baseDir.toString().replace('\\', '/');
                String relativeGlob = glob.substring(baseStr.length()).replaceFirst("^[/\\\\]+", "");
                return fs.getPathMatcher("glob:" + relativeGlob);
            }
            return fs.getPathMatcher("glob:" + glob);
    }


    public final void overwrite(@NonNull FileContent fileContent) throws IOException {
        Files.writeString(fileContent.getPath(), fileContent.getContent(), StandardCharsets.UTF_8);

        if (log.isInfoEnabled()) {
            log.info("File content overwritten at {}", fileContent);
        }
    }

    @NonNull
    public final FileContent readFile(@NonNull Path file) throws IOException {
        if (log.isInfoEnabled()) log.info("Reading file contents from {}", file);

        return new FileContent(file, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Value
    public static class FileContent {
        @NonNull
        Path path;

        @NonNull
        String content;

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FileContent that)) {
                return false;
            }
            return path.equals(that.path) && content.equals(that.content);
        }

        @Override
        public int hashCode() {
            int result = path.hashCode();
            result = 31 * result + content.hashCode();
            return result;
        }
    }

    @Value
    private static class ResolvedGlob {
        Path baseDir;
        PathMatcher matcher;

        public static ResolvedGlob from(String glob) {
            Path path = Paths.get(glob);
            boolean isAbsolute = path.isAbsolute();

            String prefix = extractNonGlobPrefix(glob);
            Path base = isAbsolute
                    ? Paths.get(prefix)
                    : Paths.get(prefix).toAbsolutePath().normalize();

            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);

            return new ResolvedGlob(base, matcher);
        }

        private static String extractNonGlobPrefix(String glob) {
            int wildcardIdx = glob.indexOf('*');
            if (wildcardIdx == -1) wildcardIdx = glob.indexOf('?');
            if (wildcardIdx == -1) return glob;

            int slashIdx = glob.lastIndexOf('/', wildcardIdx);
            return (slashIdx >= 0) ? glob.substring(0, slashIdx) : ".";
        }
    }
}
