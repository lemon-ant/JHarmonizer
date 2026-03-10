package io.github.lemon_ant.jharmonizer.core.processing_stat;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
class PathDisplayFormatUtil {

    private static final String ELLIPSIS = "...";
    private static final int MINIMAL_JAVA_FILE_NAME_LENGTH = "A.java".length();

    @NonNull
    static String abbreviatePathForDisplay(@NonNull Path path, int maxTotalLength) {
        String fullPathString = path.toString();
        if (fullPathString.length() <= maxTotalLength) {
            return fullPathString;
        }

        String fileSystemSeparator = path.getFileSystem().getSeparator();
        int abbreviationPrefixLength = ELLIPSIS.length() + fileSystemSeparator.length();

        validateMaxTotalLength(maxTotalLength, abbreviationPrefixLength);

        int nameElementCount = path.getNameCount();
        if (nameElementCount == 0) {
            // Root-only path like "C:\" or "/".
            // Current behavior: return as-is even if it exceeds maxTotalLength.
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

        return renderAbbreviatedPath(fileSystemSeparator, selectedTailElements);
    }

    private static void validateMaxTotalLength(int maxTotalLength, int abbreviationPrefixLength) {

        int minJavaPathLength = abbreviationPrefixLength + PathDisplayFormatUtil.MINIMAL_JAVA_FILE_NAME_LENGTH;

        if (maxTotalLength <= minJavaPathLength) {
            throw new IllegalArgumentException("maxTotalLength is too small to render an abbreviated Java path. "
                    + "It must be greater than " + minJavaPathLength
                    + " (abbreviation prefix " + abbreviationPrefixLength
                    + " + minimal file name length " + PathDisplayFormatUtil.MINIMAL_JAVA_FILE_NAME_LENGTH
                    + "), but was: " + maxTotalLength);
        }
    }

    private static String renderAbbreviatedPath(String fileSystemSeparator, Deque<String> selectedTailElements) {
        String abbreviatedTail = String.join(fileSystemSeparator, selectedTailElements);
        return ELLIPSIS + fileSystemSeparator + abbreviatedTail;
    }

    private static String extractFileName(Path pathToFile) {
        Path fileNamePath = pathToFile.getFileName();
        return fileNamePath == null ? "" : fileNamePath.toString();
    }
}
