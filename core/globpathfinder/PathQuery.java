package io.github.lemon_ant.globpathfinder;

import static java.util.Optional.ofNullable;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.FileVisitOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * PathQuery — immutable configuration object for {@code GlobPathFinder.findPaths(PathQuery)}.
 *
 * <p><strong>Purpose:</strong> describe base directory, include/exclude glob filters, optional
 * extension filtering, depth and onlyFiles flags in a relaxed, developer-friendly form.
 * All nullable inputs are normalized to safe defaults by the builder-constructor, so callers can omit fields.</p>
 *
 * <h2>Semantics</h2>
 * <ul>
 *   <li><b>baseDir</b> — starting directory. If {@code null} it is initialized by default with current directory
 *       via {@code Path.of(".")}.</li>
 *   <li><b>includeGlobs</b> — inclusion glob-patterns.</li>
 *   <li><b>excludeGlobs</b> — exclusion patterns applied to found paths.</li>
 *   <li><b>allowedExtensions</b> — optional case-insensitive extension filter provided without dots.
 *       Empty set disables this filter.</li>
 *   <li><b>onlyFiles</b> — return only regular files when {@code true}; otherwise files and directories.</li>
 *   <li><b>maxDepth</b> — maximum depth; negative or omitted value is treated as unlimited.</li>
 *   <li><b>followLinks</b> — handling of symbolic links: when {@code true}, traversal resolves the link and visits the
 *       target (a link to a directory is descended into; a link to a file is treated as that file for filters such as
 *       {@code onlyFiles} and extensions). Link cycles are detected and skipped; dangling links are not resolved. When
 *       {@code false}, links are visited as link entries only and are never traversed.</li>
 *   <li><b>failFastOnError</b> — error-handling behavior during traversal:
 *       {@code true} ⇒ fail-fast on the first late I/O error; {@code false} ⇒ shield errors and continue.</li>
 * </ul>
 *
 * <h2>Relaxed defaults</h2>
 * <ul>
 *   <li>{@code baseDir == null} or omitted → {@code Path.of(".")}.</li>
 *   <li>{@code includeGlobs == null} or omitted → {@code Set.of()}. An empty set means all files under the base
 *       directory are selected.</li>
 *   <li>{@code allowedExtensions/excludeGlobs == null} → {@code Set.of()}. Empty set disables the filters.</li>
 *   <li>{@code maxDepth == null || maxDepth < 0} or omitted → unlimited ({@code Integer.MAX_VALUE}).</li>
 *   <li>{@code onlyFiles == null}  or omitted → {@code true}.</li>
 *   <li>{@code followLinks == null} or omitted → {@code true}.</li>
 *   <li>{@code failFastOnError == null} or omitted → {@code true} (fail-fast).</li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * // 1) Java sources under 'src', excluding tests:
 * PathQuery q1 = PathQuery.builder()
 *     .baseDir(Paths.get("."))
 *     .includeGlobs(Set.of("src/**"))
 *     .excludeGlobs(Set.of("**" + "/test/**"))
 *     .allowedExtensions(Set.of("java"))
 *     .onlyFiles(true)
 *     .followLinks(false)
 *     .build();
 *
 * // 2) Plain directory include becomes match-all under that base:
 * PathQuery q2 = PathQuery.builder()
 *     .includeGlobs(Set.of("src")) // empty tail ⇒ MATCH_ALL under <base>/src
 *     .build();
 * }</pre>
 *
 * <p>Immutability: collections are defensively copied; getters return unmodifiable views.</p>
 */
@Value
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class PathQuery {

    /**
     * Optional whitelist of file extensions without dots, case-insensitive.
     * <br/>Normalization rules performed in the constructor:
     * <ul>
     *   <li>If the input collection is {@code null} or omitted, this field becomes an empty Set,</li>
     *   <li>otherwise it is defensively copied to an unmodifiable Set.</li>
     * </ul>
     * Behavioral notes:
     * <ul>
     *   <li>An empty Set disables the extension filter entirely.</li>
     * </ul>
     */
    @NonNull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    Set<String> allowedExtensions;

    /**
     * Starting directory for traversal.
     * If null or omitted in the builder, it becomes Path.of(".") in the constructor.
     */
    @NonNull
    Path baseDir;

    /**
     * Optional exclude glob patterns.
     * <br/>Normalization rules performed in the constructor:
     * <ul>
     *   <li>If the input collection is {@code null} or omitted, this field becomes an empty Set,</li>
     *   <li>otherwise it is defensively copied to an unmodifiable Set.</li>
     * </ul>
     * Behavioral notes:
     * <ul>
     *   <li>An empty Set disables exclude filtering.</li>
     * </ul>
     */
    @NonNull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    Set<String> excludeGlobs;

    /**
     * Error-handling strategy flag.
     * <ul>
     *   <li>When {@code true}, traversal is fail-fast: the first late I/O error
     *       (e.g. AccessDeniedException, UncheckedIOException) immediately throws and aborts the entire pipeline.</li>
     *   <li>When {@code false}, traversal is shielded: errors are logged at WARN level
     *       and only the current branch is cut, the rest of the pipeline continues.</li>
     * </ul>
     * If null or omitted in the builder, defaults to {@code true} (fail-fast).
     */
    boolean failFastOnError;

    /**
     * Whether to follow symbolic links. If null or omitted in the builder, defaults to true (symbolic links are
     * followed).
     */
    boolean followLinks;

    /**
     * Include glob patterns.
     * <br/>Normalization rules performed in the constructor:
     * <ul>
     *   <li>If the input collection is {@code null} or omitted, this field becomes an empty Set,</li>
     *   <li>otherwise it is defensively copied to an unmodifiable Set.</li>
     * </ul>
     * Behavior in the finder:
     * <ul>
     *   <li>An empty include set is interpreted as "match all under baseDir".</li>
     * </ul>
     */
    @NonNull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    Set<String> includeGlobs;

    /**
     * Maximum depth to traverse. If null or omitted or negative in the builder, becomes Integer.MAX_VALUE (unlimited).
     */
    int maxDepth;

    /**
     * Whether to match only regular files (true) or everything (false). If null or omitted in the builder,
     * defaults to true (only regular files are returned).
     */
    boolean onlyFiles;

    /**
     * Builder-backed constructor. Normalizes nullable inputs to safe defaults.
     *
     * @param baseDir           Starting directory. If null or omitted in the PathQuery builder, becomes the current
     *                          working directory Path.of(".").
     * @param includeGlobs      Include glob patterns. If null or omitted, becomes an empty Set, otherwise defensively
     *                          copied to unmodifiable Set. An empty include set means "match all under baseDir".
     * @param allowedExtensions Allowed file extensions without dots, case-insensitive. If null or omitted, becomes an
     *                          empty Set (disable filter), otherwise defensively copied to unmodifiable Set.
     * @param excludeGlobs      Exclude glob patterns. If null or omitted, becomes an empty Set (disable filter),
     *                          otherwise defensively copied to unmodifiable Set.
     * @param maxDepth          Maximum depth. If null or omitted or negative, becomes Integer.MAX_VALUE (unlimited).
     * @param onlyFiles         If null or omitted, defaults to true (only regular files are returned).
     * @param followLinks       If null or omitted, defaults to true (symbolic links are followed).
     * @param failFastOnError   Error-handling strategy. If null or omitted, defaults to true (fail-fast).
     *                          true ⇒ abort immediately on first I/O error,
     *                          false ⇒ shield errors, log a warning, and continue traversal.
     */
    @Builder(toBuilder = true)
    private PathQuery(
            @Nullable Path baseDir,
            @Nullable Collection<String> includeGlobs,
            @Nullable Collection<String> allowedExtensions,
            @Nullable Collection<String> excludeGlobs,
            @Nullable Integer maxDepth,
            @Nullable Boolean onlyFiles,
            @Nullable Boolean followLinks,
            @Nullable Boolean failFastOnError) {
        this.baseDir = ofNullable(baseDir).orElse(Path.of("."));
        this.includeGlobs = ofNullable(includeGlobs).map(Set::copyOf).orElse(Set.of());
        this.allowedExtensions = ofNullable(allowedExtensions).map(Set::copyOf).orElse(Set.of());
        this.excludeGlobs = ofNullable(excludeGlobs).map(Set::copyOf).orElse(Set.of());
        this.maxDepth = ofNullable(maxDepth).filter(depth -> depth >= 0).orElse(Integer.MAX_VALUE);
        this.onlyFiles = ofNullable(onlyFiles).orElse(true);
        this.followLinks = ofNullable(followLinks).orElse(true);
        this.failFastOnError = ofNullable(failFastOnError).orElse(true);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PathQuery)) {
            return false;
        }

        PathQuery pathQuery = (PathQuery) o;
        return maxDepth == pathQuery.maxDepth
                && onlyFiles == pathQuery.onlyFiles
                && followLinks == pathQuery.followLinks
                && baseDir.equals(pathQuery.baseDir)
                && includeGlobs.equals(pathQuery.includeGlobs)
                && allowedExtensions.equals(pathQuery.allowedExtensions)
                && excludeGlobs.equals(pathQuery.excludeGlobs);
    }

    /**
     * Computes the visit options derived from this configuration.
     * <br/>Current behavior:
     * <ul>
     *   <li>If {@code followLinks} is true, returns {@code EnumSet.of(FileVisitOption.FOLLOW_LINKS)}.</li>
     *   <li>Otherwise, returns {@code Set.of()} — no options.</li>
     * </ul>
     *
     * @return a Set of FileVisitOption reflecting the {@code followLinks} flag only
     */
    public Set<FileVisitOption> getVisitOptions() {
        return followLinks ? EnumSet.of(FileVisitOption.FOLLOW_LINKS) : Set.of();
    }

    @Override
    public int hashCode() {
        int result = baseDir.hashCode();
        result = 31 * result + includeGlobs.hashCode();
        result = 31 * result + allowedExtensions.hashCode();
        result = 31 * result + excludeGlobs.hashCode();
        result = 31 * result + maxDepth;
        result = 31 * result + Boolean.hashCode(onlyFiles);
        result = 31 * result + Boolean.hashCode(followLinks);
        return result;
    }
}
