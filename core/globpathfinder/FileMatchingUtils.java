package io.github.lemon_ant.globpathfinder;

import static java.util.Optional.ofNullable;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
class FileMatchingUtils {
    private static final PathMatcher MATCH_ALL = FileSystems.getDefault().getPathMatcher("glob:**");
    private static final Pattern WINDOWS_DRIVE_PATTERN = Pattern.compile("^[a-zA-Z]:[/\\\\].*");

    @NonNull
    static Map<Path, Set<PathMatcher>> computeBaseToIncludeMatchers(
            @NonNull Path baseDir, @NonNull Set<String> includeGlobs) {
        Map<Path, Set<PathMatcher>> result = includeGlobs.stream()
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .map(glob -> extractBaseAndPattern(baseDir, glob))
                .collect(Collectors.groupingBy(
                        Pair::getKey,
                        Collectors.collectingAndThen(
                                Collectors.mapping(Pair::getValue, Collectors.toUnmodifiableSet()),
                                set -> set.contains(MATCH_ALL) ? Set.of() : set)));
        if (!result.isEmpty()) {
            return result;
        }
        return Map.of(baseDir, Set.of());
    }

    static boolean isMatchedToPatterns(@NonNull Path pathToMatch, @NonNull Set<PathMatcher> pathMatchers) {
        return pathMatchers.isEmpty() || pathMatchers.stream().anyMatch(matcher -> matcher.matches(pathToMatch));
    }

    static Pair<List<String>, List<String>> partitionAbsoluteAndRelative(Collection<String> patterns) {
        List<String> absolute = new ArrayList<>();
        List<String> relative = new ArrayList<>();

        for (String pattern : patterns) {
            if (isAbsoluteGlob(pattern)) {
                absolute.add(pattern);
            } else {
                relative.add(pattern);
            }
        }
        return Pair.of(absolute, relative);
    }

    @Nullable
    private static String composePattern(int startSegment, String[] segments, boolean addTrailSlash) {
        if (startSegment == segments.length) {
            return null;
        }
        StringBuilder patternBuilder = new StringBuilder();
        for (int j = startSegment; j < segments.length; j++) {
            if (patternBuilder.length() > 0) {
                patternBuilder.append('/');
            }
            patternBuilder.append(segments[j]);
        }
        if (addTrailSlash) {
            patternBuilder.append('/');
        }
        return patternBuilder.toString();
    }

    @NonNull
    private static Pair<Path, PathMatcher> extractBaseAndPattern(Path defaultAbsoluteBase, String globPattern) {
        String normalizedGlob = globPattern.replace('\\', '/');
        String[] pathSegments = StringUtils.split(normalizedGlob, '/');

        StringBuilder baseBuilder = new StringBuilder();

        if (!normalizedGlob.isEmpty() && normalizedGlob.charAt(0) == '/') {
            // Absolute path started from root slash
            baseBuilder.append(File.separatorChar);
        }

        // Find fixed start path
        int segmentIdx = 0;
        for (; segmentIdx < pathSegments.length; segmentIdx++) {
            if (isWildcardSegment(pathSegments[segmentIdx])) {
                break;
            }
            if (segmentIdx > 0) {
                baseBuilder.append(File.separatorChar);
            }
            baseBuilder.append(pathSegments[segmentIdx]);
        }

        String staticRoot = baseBuilder.toString();
        Path extractedBasePath;
        if (staticRoot.isEmpty()) {
            extractedBasePath = defaultAbsoluteBase;
        } else {
            Path staticRootPath = Path.of(staticRoot).normalize();
            if (staticRootPath.isAbsolute()) {
                extractedBasePath = staticRootPath;
            } else {
                extractedBasePath = defaultAbsoluteBase.resolve(staticRootPath).toAbsolutePath();
            }
        }

        String pattern =
                composePattern(segmentIdx, pathSegments, normalizedGlob.charAt(normalizedGlob.length() - 1) == '/');

        return Pair.of(
                extractedBasePath,
                ofNullable(pattern)
                        .map(ptr -> FileSystems.getDefault().getPathMatcher("glob:" + ptr))
                        .orElse(MATCH_ALL));
    }

    private static boolean isAbsoluteGlob(String globPattern) {
        // Normalize separators
        String normalized = globPattern.replace('\\', '/');

        // Unix-like absolute OR Windows drive letter absolute (e.g., C:/, D:\)
        return normalized.startsWith("/")
                || WINDOWS_DRIVE_PATTERN.matcher(globPattern).matches();
    }

    private static boolean isWildcardSegment(String segment) {
        return StringUtils.containsAny(segment, "*?[{]}");
    }
}
