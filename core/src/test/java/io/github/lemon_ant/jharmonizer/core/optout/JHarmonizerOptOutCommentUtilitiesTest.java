package io.github.lemon_ant.jharmonizer.core.optout;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.optout.JHarmonizerOptOutCommentUtilities.RawCommentMatch;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtComment.CommentType;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;
import spoon.support.DefaultCoreFactory;
import spoon.support.StandardEnvironment;

class JHarmonizerOptOutCommentUtilitiesTest {

    @Test
    void parseFileScopeOptOutMode_noDirectiveComment_returnsNull() {
        // Given
        SrcFile srcFile = createSrcFile("class A {}", Path.of("A.java"));

        // When
        JHarmonizerOptOutMode result =
                JHarmonizerOptOutCommentUtilities.parseFileScopeOptOutMode("// normal comment", 0, srcFile);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void parseFileScopeOptOutMode_fullyOffLineComment_returnsFullyOffMode() {
        // Given
        SrcFile srcFile = createSrcFile("class A {}", Path.of("A.java"));

        // When
        JHarmonizerOptOutMode result =
                JHarmonizerOptOutCommentUtilities.parseFileScopeOptOutMode("// @jharmonizer:fully-off", 0, srcFile);

        // Then
        assertThat(result).isEqualTo(JHarmonizerOptOutMode.FULLY_OFF);
    }

    @Test
    void parseFileScopeOptOutMode_sortOffBlockComment_returnsSortingOffMode() {
        // Given
        SrcFile srcFile = createSrcFile("class A {}", Path.of("A.java"));

        // When
        JHarmonizerOptOutMode result =
                JHarmonizerOptOutCommentUtilities.parseFileScopeOptOutMode("/* @jharmonizer:sort-off */", 0, srcFile);

        // Then
        assertThat(result).isEqualTo(JHarmonizerOptOutMode.SORTING_OFF);
    }

    @Test
    void parseFileScopeOptOutMode_javadocComment_returnsNull() {
        // Given
        SrcFile srcFile = createSrcFile("class A {}", Path.of("A.java"));

        // When
        JHarmonizerOptOutMode result =
                JHarmonizerOptOutCommentUtilities.parseFileScopeOptOutMode("/** @jharmonizer:fully-off */", 0, srcFile);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void parseFileScopeOptOutMode_directiveNotAtStart_returnsNull() {
        // Given
        SrcFile srcFile = createSrcFile("class A {}", Path.of("A.java"));

        // When
        JHarmonizerOptOutMode result = JHarmonizerOptOutCommentUtilities.parseFileScopeOptOutMode(
                "// some text @jharmonizer:fully-off", 0, srcFile);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void parseFileScopeOptOutMode_unknownToken_returnsNull() {
        // Given
        SrcFile srcFile = createSrcFile("class A {}", Path.of("A.java"));

        // When
        JHarmonizerOptOutMode result =
                JHarmonizerOptOutCommentUtilities.parseFileScopeOptOutMode("// @jharmonizer:unknown-mode", 0, srcFile);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void collectRawCommentsByRegex_srcWithLineAndBlockComments_returnsAllComments() {
        // Given
        String srcCode = "// line comment\nclass A { /* block comment */ }";

        // When
        List<RawCommentMatch> rawCommentMatches = JHarmonizerOptOutCommentUtilities.collectRawCommentsByRegex(srcCode);

        // Then
        assertThat(rawCommentMatches).hasSize(2);
        assertThat(rawCommentMatches.get(0).getRawComment()).isEqualTo("// line comment\n");
        assertThat(rawCommentMatches.get(1).getRawComment()).isEqualTo("/* block comment */");
    }

    @Test
    void collectRawCommentsByRegex_emptySource_returnsEmptyList() {
        // When
        List<RawCommentMatch> rawCommentMatches = JHarmonizerOptOutCommentUtilities.collectRawCommentsByRegex("");

        // Then
        assertThat(rawCommentMatches).isEmpty();
    }

    @Test
    void formatLocation_srcOffsetWithNewlines_correctlyComputesLineAndColumn() {
        // Given
        String srcCode = "line1\nline2\nline3";
        SrcFile srcFile = createSrcFile(srcCode, Path.of("Sample.java"));
        int offsetOnLine2 = "line1\n".length() + 2;

        // When
        String location = JHarmonizerOptOutCommentUtilities.formatLocation(srcFile, offsetOnLine2);

        // Then
        assertThat(location).startsWith("Sample.java:2:");
    }

    @Test
    void formatLocation_offsetZero_returnsLine1Column1() {
        // Given
        SrcFile srcFile = createSrcFile("class A {}", Path.of("A.java"));

        // When
        String location = JHarmonizerOptOutCommentUtilities.formatLocation(srcFile, 0);

        // Then
        assertThat(location).isEqualTo("A.java:1:1");
    }

    @Test
    void parseTypeOptOutMode_javadocComment_returnsNull() {
        // Given
        Factory factory = new FactoryImpl(new DefaultCoreFactory(), new StandardEnvironment());
        CtComment javadocComment = factory.createComment("@jharmonizer:fully-off", CommentType.JAVADOC);

        // When
        JHarmonizerOptOutMode result = JHarmonizerOptOutCommentUtilities.parseTypeOptOutMode(javadocComment);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void parseTypeOptOutMode_directiveNotAtStart_returnsNull() {
        // Given
        Factory factory = new FactoryImpl(new DefaultCoreFactory(), new StandardEnvironment());
        CtComment comment = factory.createComment("some text @jharmonizer:fully-off", CommentType.INLINE);

        // When
        JHarmonizerOptOutMode result = JHarmonizerOptOutCommentUtilities.parseTypeOptOutMode(comment);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void parseTypeOptOutMode_unknownToken_returnsNull() {
        // Given
        Factory factory = new FactoryImpl(new DefaultCoreFactory(), new StandardEnvironment());
        CtComment comment = factory.createComment("@jharmonizer:invalid-mode", CommentType.INLINE);

        // When
        JHarmonizerOptOutMode result = JHarmonizerOptOutCommentUtilities.parseTypeOptOutMode(comment);

        // Then
        assertThat(result).isNull();
    }
}
