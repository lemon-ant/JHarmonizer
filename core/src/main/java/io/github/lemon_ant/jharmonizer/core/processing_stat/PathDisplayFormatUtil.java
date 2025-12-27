package io.github.lemon_ant.jharmonizer.core.processing_stat;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import lombok.experimental.UtilityClass;

@UtilityClass
class PathDisplayFormatUtil {

    private static final String ELLIPSIS = "…";
    private static final int MINIMAL_JAVA_FILE_NAME_LENGTH = "A.java".length();

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

        String fileSystemSeparator = path.getFileSystem().getSeparator();
        int abbreviationPrefixLength = ELLIPSIS.length() + fileSystemSeparator.length();
        int minJavaPathLength = abbreviationPrefixLength + MINIMAL_JAVA_FILE_NAME_LENGTH;

        if (maxTotalLength <= minJavaPathLength) {
            throw new IllegalArgumentException("maxTotalLength is too small to render an abbreviated Java path. "
                    + "It must be greater than " + minJavaPathLength
                    + " (abbreviation prefix " + abbreviationPrefixLength
                    + " + minimal file name length " + MINIMAL_JAVA_FILE_NAME_LENGTH
                    + "), but was: " + maxTotalLength);
        }

        int nameElementCount = path.getNameCount();
        if (nameElementCount == 0) {
            // Root-only path like "C:\" or "/": keep the rightmost part within limit.
            return fullPathString;
        }

        int availableTailLength = maxTotalLength - abbreviationPrefixLength;
        String fileName = extractFileName(path);

        Deque<String> selectedTailElements = new ArrayDeque<>();
        selectedTailElements.addFirst(fileName);

        int selectedTailLength = fileName.length();
        int separatorLength = fileSystemSeparator.length();

        // Add as many parent segments as fit.
        for (int nameIndex = nameElementCount - 2; nameIndex >= 0; nameIndex--) {
            String candidateElement = path.getName(nameIndex).toString();
            int candidateCost = separatorLength + candidateElement.length();
            if (selectedTailLength + candidateCost > availableTailLength) {
                break;
            }
            selectedTailElements.addFirst(candidateElement);
            selectedTailLength += candidateCost;
        }

        String abbreviatedTail = String.join(fileSystemSeparator, selectedTailElements);
        return ELLIPSIS + fileSystemSeparator + abbreviatedTail;
    }

    private static String extractFileName(Path pathToFile) {
        Path fileNamePath = pathToFile.getFileName();
        return fileNamePath == null ? "" : fileNamePath.toString();
    }
}
