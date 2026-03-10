package io.github.lemon_ant.jharmonizer.core.processing_stat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PathDisplayFormatUtilTest {

    @Test
    void abbreviatePathForDisplay_pathFitsWithinLimit_returnFullPath() {
        // Given
        Path path = Path.of("some/short/path/File.java");
        int maxTotalLength = 100;

        // When
        String result = PathDisplayFormatUtil.abbreviatePathForDisplay(path, maxTotalLength);

        // Then
        assertThat(result).isEqualTo("some/short/path/File.java");
    }

    @Test
    void abbreviatePathForDisplay_pathExactlyAtLimit_returnFullPath() {
        // Given
        String pathString = "some/short/path/File.java";
        Path path = Path.of(pathString);
        int maxTotalLength = pathString.length();

        // When
        String result = PathDisplayFormatUtil.abbreviatePathForDisplay(path, maxTotalLength);

        // Then
        assertThat(result).isEqualTo(pathString);
    }

    @Test
    void abbreviatePathForDisplay_pathExceedsLimit_onlyFileNameFits_returnEllipsisWithFileName() {
        // Given
        // Full path "a/b/c/d/segment/MyClass.java" is 28 chars; maxTotalLength is 16.
        // availableTailLength = 16 - 4 (".../" prefix) = 12.
        // "MyClass.java" = 12 chars → fits exactly.
        // "segment" costs sep(1) + 7 = 8; 12 + 8 = 20 > 12 → does not fit.
        Path path = Path.of("a/b/c/d/segment/MyClass.java");
        int maxTotalLength = 16;

        // When
        String result = PathDisplayFormatUtil.abbreviatePathForDisplay(path, maxTotalLength);

        // Then
        assertThat(result).isEqualTo(".../MyClass.java");
        assertThat(result.length()).isLessThanOrEqualTo(maxTotalLength);
    }

    @Test
    void abbreviatePathForDisplay_pathExceedsLimit_fileNameAndOneParentFit_returnEllipsisWithParentAndFileName() {
        // Given
        // Full path "some/very/long/path/parent/MyClass.java" is 39 chars; maxTotalLength is 23.
        // availableTailLength = 23 - 4 (".../" prefix) = 19.
        // "MyClass.java" = 12; "parent" costs sep(1) + 6 = 7; 12 + 7 = 19 → fits exactly.
        // "path" would cost sep(1) + 4 = 5; 19 + 5 = 24 > 19 → does not fit.
        Path path = Path.of("some/very/long/path/parent/MyClass.java");
        int maxTotalLength = 23;

        // When
        String result = PathDisplayFormatUtil.abbreviatePathForDisplay(path, maxTotalLength);

        // Then
        assertThat(result).isEqualTo(".../parent/MyClass.java");
        assertThat(result.length()).isLessThanOrEqualTo(maxTotalLength);
    }

    @Test
    void abbreviatePathForDisplay_pathExceedsLimit_multipleParentsFit_allIncluded() {
        // Given
        // Full path "a/b/c/d/e/f/MyClass.java" is 24 chars; maxTotalLength is 22.
        // availableTailLength = 22 - 4 (".../" prefix) = 18.
        // "MyClass.java" = 12; "f" costs 1+1=2 → 14; "e" costs 1+1=2 → 16; "d" costs 1+1=2 → 18.
        // "c" would cost 1+1=2 → 20 > 18 → does not fit.
        Path path = Path.of("a/b/c/d/e/f/MyClass.java");
        int maxTotalLength = 22;

        // When
        String result = PathDisplayFormatUtil.abbreviatePathForDisplay(path, maxTotalLength);

        // Then
        assertThat(result).isEqualTo(".../d/e/f/MyClass.java");
        assertThat(result.length()).isLessThanOrEqualTo(maxTotalLength);
    }

    @Test
    void abbreviatePathForDisplay_maxTotalLengthTooSmall_throwsIllegalArgumentException() {
        // Given
        // On Unix: abbreviationPrefixLength = 3 ("...") + 1 ("/") = 4.
        // minJavaPathLength = 4 + 6 ("A.java") = 10.
        // maxTotalLength = 10 is not greater than 10, so it must throw.
        Path path = Path.of("some/long/path/to/MyClass.java");
        int maxTotalLength = 10;

        // When / Then
        assertThatThrownBy(() -> PathDisplayFormatUtil.abbreviatePathForDisplay(path, maxTotalLength))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxTotalLength is too small");
    }

    @Test
    void abbreviatePathForDisplay_abbreviatedResult_startsWithAsciiEllipsis() {
        // Given
        Path path = Path.of("some/very/long/path/that/exceeds/the/limit/MyClass.java");
        int maxTotalLength = 20;

        // When
        String result = PathDisplayFormatUtil.abbreviatePathForDisplay(path, maxTotalLength);

        // Then
        assertThat(result).startsWith("...");
        assertThat(result.length()).isLessThanOrEqualTo(maxTotalLength);
    }
}
