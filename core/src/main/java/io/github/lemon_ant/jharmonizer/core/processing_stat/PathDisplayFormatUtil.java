package io.github.lemon_ant.jharmonizer.core.processing_stat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
final class PathDisplayFormatUtil {

    private static final String ELLIPSIS = "…";

    @SuppressWarnings("PMD.CyclomaticComplexity")
    static String abbreviatePathForDisplay(Path path, int maxTotalLength) {
        if (path == null) {
            return "";
        }
        if (maxTotalLength <= 0) {
            throw new IllegalArgumentException("maxTotalLength must be positive, but was: " + maxTotalLength);
        }

        String fullPathString = path.toString();
        if (fullPathString.length() <= maxTotalLength) {
            return fullPathString;
        }

        int nameElementCount = path.getNameCount();
        if (nameElementCount == 0) {
            // Root-only path like "C:\" or "/"
            return fullPathString;
        }

        String fileSystemSeparator = path.getFileSystem().getSeparator();
        int abbreviationPrefixLength = ELLIPSIS.length() + fileSystemSeparator.length(); // "…\"
        int availableTailLength = maxTotalLength - abbreviationPrefixLength;

        if (availableTailLength <= 0) {
            // No room for tail segments. Show only ".../<fileName.ext>" (shorten file name if needed).
            String fileName = extractFileName(path);
            return ELLIPSIS + fileSystemSeparator + fileName;
        }

        List<String> selectedTailElements = new ArrayList<>();
        int selectedTailLength = 0;

        for (int nameIndex = nameElementCount - 1; nameIndex >= 0; nameIndex--) {
            String candidateElement = path.getName(nameIndex).toString();
            int separatorCost = selectedTailElements.isEmpty() ? 0 : fileSystemSeparator.length();
            int candidateCost = separatorCost + candidateElement.length();

            if (selectedTailLength + candidateCost > availableTailLength) {
                break;
            }

            selectedTailElements.addFirst(candidateElement);
            selectedTailLength += candidateCost;
        }

        if (selectedTailElements.isEmpty()) {
            String fileName = extractFileName(path);
            return ELLIPSIS + fileSystemSeparator + fileName;
        }

        String joinedTail = String.join(fileSystemSeparator, selectedTailElements);
        String abbreviatedPath = ELLIPSIS + fileSystemSeparator + joinedTail;

        if (abbreviatedPath.length() <= maxTotalLength) {
            return abbreviatedPath;
        }

        // Safety net (should be rare): keep the rightmost part within limit.
        int keepLength = Math.max(0, maxTotalLength - ELLIPSIS.length());
        return ELLIPSIS + abbreviatedPath.substring(Math.max(0, abbreviatedPath.length() - keepLength));
    }

    private static String extractFileName(Path pathToFile) {
        Path fileNamePath = pathToFile.getFileName();
        return fileNamePath == null ? "" : fileNamePath.toString();
    }
}
