package io.github.lemon_ant.jharmonizer.core;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Renders a structured startup banner that summarises all active processing parameters.
 * The output is intended to be logged once at INFO level at the very start of processing.
 */
@UtilityClass
class StartupBannerRenderer {

    private static final String HEADER = "JHarmonizer started";
    private static final int LABEL_WIDTH = "Base directories:".length() + 1;
    private static final String LABEL_FORMAT = "%-" + LABEL_WIDTH + "s";
    private static final String GLOB_CONTINUATION_INDENT = " ".repeat(LABEL_WIDTH);

    /**
     * Builds a multiline startup banner describing the active processing parameters.
     *
     * @param flowType       the processing flow
     * @param baseDirs       resolved base directories
     * @param backupsEnabled whether backup creation is active
     * @param includeGlobs   include glob patterns (empty means "all files")
     * @param excludeGlobs   exclude glob patterns (empty means "no exclusions")
     * @return a multiline string ready for {@code log.info(...)}
     */
    @NonNull
    static String render(
            @NonNull FlowType flowType,
            @NonNull Collection<Path> baseDirs,
            boolean backupsEnabled,
            @NonNull Collection<String> includeGlobs,
            @NonNull Collection<String> excludeGlobs) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(HEADER);
        lines.add("=".repeat(HEADER.length()));
        lines.add(renderRow("Flow:", flowType.name()));
        addBaseDirRows(lines, baseDirs);
        lines.add(renderRow("Backups:", backupsEnabled ? "enabled" : "disabled"));
        addGlobRows(lines, "Include globs:", includeGlobs, "(all)");
        addGlobRows(lines, "Exclude globs:", excludeGlobs, "(none)");
        return String.join(System.lineSeparator(), lines);
    }

    @NonNull
    private static String renderRow(@NonNull String label, @NonNull String value) {
        return String.format(LABEL_FORMAT, label) + value;
    }

    private static void addBaseDirRows(@NonNull List<String> lines, @NonNull Collection<Path> baseDirs) {
        String label = baseDirs.size() == 1 ? "Base directory:" : "Base directories:";
        List<String> sortedPaths =
                baseDirs.stream().map(Path::toString).sorted().toList();
        lines.add(renderRow(label, sortedPaths.getFirst()));
        for (int pathIndex = 1; pathIndex < sortedPaths.size(); pathIndex++) {
            lines.add(GLOB_CONTINUATION_INDENT + sortedPaths.get(pathIndex));
        }
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
        List<String> sortedGlobs = globs.stream().sorted().toList();
        lines.add(renderRow(label, sortedGlobs.getFirst()));
        for (int globIndex = 1; globIndex < sortedGlobs.size(); globIndex++) {
            lines.add(GLOB_CONTINUATION_INDENT + sortedGlobs.get(globIndex));
        }
    }
}
