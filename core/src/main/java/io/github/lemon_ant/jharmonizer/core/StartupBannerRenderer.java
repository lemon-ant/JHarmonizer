package io.github.lemon_ant.jharmonizer.core;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Renders a structured startup banner that summarises all active processing parameters.
 * The output is intended to be logged once at INFO level at the very start of processing.
 */
@UtilityClass
class StartupBannerRenderer {

    private static final String HEADER = "JHarmonizer started";
    private static final int LABEL_WIDTH = "Base directory:".length() + 1;
    private static final String LABEL_FORMAT = "%-" + LABEL_WIDTH + "s";
    private static final String GLOB_CONTINUATION_INDENT = " ".repeat(LABEL_WIDTH);
    private static final char BACKSLASH = '\\';
    private static final Set<Character> GLOB_METACHARS = Set.of('*', '?', '[', ']', '{', '}', BACKSLASH);

    /**
     * Builds a multiline startup banner describing the active processing parameters.
     *
     * @param flowType       the processing flow
     * @param baseDir        the resolved base directory
     * @param backupsEnabled whether backup creation is active
     * @param includeGlobs   include glob patterns (empty means "all files")
     * @param excludeGlobs   exclude glob patterns (empty means "no exclusions")
     * @return a multiline string ready for {@code log.info(...)}
     */
    @NonNull
    static String render(
            @NonNull FlowType flowType,
            @NonNull Path baseDir,
            boolean backupsEnabled,
            @NonNull Collection<String> includeGlobs,
            @NonNull Collection<String> excludeGlobs) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(HEADER);
        lines.add("=".repeat(HEADER.length()));
        lines.add(renderRow("Flow:", flowType.name()));
        lines.add(renderRow("Base directory:", baseDir.toString()));
        lines.add(renderRow("Backups:", backupsEnabled ? "enabled" : "disabled"));
        addGlobRows(lines, "Include globs:", includeGlobs, "(all)");
        addGlobRows(lines, "Exclude globs:", excludeGlobs, "(none)");
        return String.join(System.lineSeparator(), lines);
    }

    @NonNull
    private static String renderRow(@NonNull String label, @NonNull String value) {
        return String.format(LABEL_FORMAT, label) + value;
    }

    private static void addGlobRows(
            @NonNull List<String> lines,
            @NonNull String label,
            @NonNull Collection<String> globs,
            @NonNull String emptyPlaceholder) {
        if (globs.isEmpty()) {
            lines.add(renderRow(label, emptyPlaceholder));
            return;
        }
        List<String> sortedGlobs = globs.stream()
                .map(StartupBannerRenderer::normalizeGlobSeparators)
                .sorted()
                .toList();
        lines.add(renderRow(label, sortedGlobs.getFirst()));
        for (int globIndex = 1; globIndex < sortedGlobs.size(); globIndex++) {
            lines.add(GLOB_CONTINUATION_INDENT + sortedGlobs.get(globIndex));
        }
    }

    /**
     * Normalizes path-separator backslashes in a glob pattern to forward slashes for consistent
     * display. A backslash that precedes a glob metacharacter ({@code * ? [ ] { } \}) is an
     * escape sequence and is left unchanged; all other backslashes are treated as path separators
     * and replaced with {@code /}.
     *
     * @param glob the raw glob pattern
     * @return the pattern with path-separator backslashes replaced by forward slashes
     */
    @NonNull
    private static String normalizeGlobSeparators(String glob) {
        StringBuilder result = new StringBuilder(glob.length());
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == BACKSLASH && i + 1 < glob.length() && isGlobMetachar(glob.charAt(i + 1))) {
                result.append(c);
            } else if (c == BACKSLASH) {
                result.append('/');
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static boolean isGlobMetachar(char c) {
        return GLOB_METACHARS.contains(c);
    }
}
