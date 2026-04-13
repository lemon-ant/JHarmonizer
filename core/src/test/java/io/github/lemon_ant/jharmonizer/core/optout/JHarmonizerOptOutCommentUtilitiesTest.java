package io.github.lemon_ant.jharmonizer.core.optout;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtComment.CommentType;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;
import spoon.support.DefaultCoreFactory;
import spoon.support.StandardEnvironment;

class JHarmonizerOptOutCommentUtilitiesTest {

    private static final Path SAMPLE_PATH = Path.of("Sample.java");

    @Nested
    class ParseTypeOptOutMode {

        @Test
        void parseTypeOptOutMode_inlineCommentWithNoToken_returnsNull() {
            // Given
            CtComment comment = createInlineComment("just a regular comment");

            // When
            JHarmonizerOptOutMode optOutMode = JHarmonizerOptOutCommentUtilities.parseTypeOptOutMode(comment);

            // Then
            assertThat(optOutMode).isNull();
        }

        @Test
        void parseTypeOptOutMode_javadocCommentWithToken_returnsNull() {
            // Given
            CtComment comment = createComment("@jharmonizer:fully-off", CommentType.JAVADOC);

            // When
            JHarmonizerOptOutMode optOutMode = JHarmonizerOptOutCommentUtilities.parseTypeOptOutMode(comment);

            // Then
            assertThat(optOutMode).isNull();
        }

        @Test
        void parseTypeOptOutMode_inlineCommentWithTokenNotAtStart_returnsNull() {
            // Given
            CtComment comment = createInlineComment("some text @jharmonizer:fully-off");

            // When
            JHarmonizerOptOutMode optOutMode = JHarmonizerOptOutCommentUtilities.parseTypeOptOutMode(comment);

            // Then
            assertThat(optOutMode).isNull();
        }

        @Test
        void parseTypeOptOutMode_inlineCommentWithValidToken_returnsMode() {
            // Given
            CtComment comment = createInlineComment("@jharmonizer:fully-off");

            // When
            JHarmonizerOptOutMode optOutMode = JHarmonizerOptOutCommentUtilities.parseTypeOptOutMode(comment);

            // Then
            assertThat(optOutMode).isEqualTo(JHarmonizerOptOutMode.FULLY_OFF);
        }

        @Test
        void parseTypeOptOutMode_inlineCommentWithSortOffToken_returnsMode() {
            // Given
            CtComment comment = createInlineComment("@jharmonizer:sort-off");

            // When
            JHarmonizerOptOutMode optOutMode = JHarmonizerOptOutCommentUtilities.parseTypeOptOutMode(comment);

            // Then
            assertThat(optOutMode).isEqualTo(JHarmonizerOptOutMode.SORTING_OFF);
        }

        @Test
        void parseTypeOptOutMode_inlineCommentWithUnknownToken_returnsNull() {
            // Given
            CtComment comment = createInlineComment("@jharmonizer:unknown-token");

            // When
            JHarmonizerOptOutMode optOutMode = JHarmonizerOptOutCommentUtilities.parseTypeOptOutMode(comment);

            // Then
            assertThat(optOutMode).isNull();
        }

        @Test
        void parseTypeOptOutMode_blockCommentWithValidToken_returnsMode() {
            // Given
            CtComment comment = createComment("@jharmonizer:fully-off", CommentType.BLOCK);

            // When
            JHarmonizerOptOutMode optOutMode = JHarmonizerOptOutCommentUtilities.parseTypeOptOutMode(comment);

            // Then
            assertThat(optOutMode).isEqualTo(JHarmonizerOptOutMode.FULLY_OFF);
        }
    }

    @Nested
    class ParseFileScopeOptOutMode {

        @Test
        void parseFileScopeOptOutMode_commentWithNoToken_returnsNull() {
            // Given
            String rawComment = "// just a regular comment";

            // When
            JHarmonizerOptOutMode optOutMode = JHarmonizerOptOutCommentUtilities.parseFileScopeOptOutMode(
                    rawComment, 0, createSrcFile("package demo;", SAMPLE_PATH));

            // Then
            assertThat(optOutMode).isNull();
        }

        @Test
        void parseFileScopeOptOutMode_javadocCommentWithToken_returnsNull() {
            // Given
            String rawComment = "/** @jharmonizer:fully-off */";

            // When
            JHarmonizerOptOutMode optOutMode = JHarmonizerOptOutCommentUtilities.parseFileScopeOptOutMode(
                    rawComment, 0, createSrcFile("package demo;", SAMPLE_PATH));

            // Then
            assertThat(optOutMode).isNull();
        }

        @Test
        void parseFileScopeOptOutMode_commentWithTokenNotAtStart_returnsNull() {
            // Given
            String rawComment = "// some prefix @jharmonizer:fully-off";

            // When
            JHarmonizerOptOutMode optOutMode = JHarmonizerOptOutCommentUtilities.parseFileScopeOptOutMode(
                    rawComment, 0, createSrcFile("package demo;", SAMPLE_PATH));

            // Then
            assertThat(optOutMode).isNull();
        }

        @Test
        void parseFileScopeOptOutMode_validInlineComment_returnsMode() {
            // Given
            String rawComment = "// @jharmonizer:fully-off";

            // When
            JHarmonizerOptOutMode optOutMode = JHarmonizerOptOutCommentUtilities.parseFileScopeOptOutMode(
                    rawComment, 0, createSrcFile("package demo;", SAMPLE_PATH));

            // Then
            assertThat(optOutMode).isEqualTo(JHarmonizerOptOutMode.FULLY_OFF);
        }

        @Test
        void parseFileScopeOptOutMode_validBlockComment_returnsMode() {
            // Given
            String rawComment = "/* @jharmonizer:sort-off */";

            // When
            JHarmonizerOptOutMode optOutMode = JHarmonizerOptOutCommentUtilities.parseFileScopeOptOutMode(
                    rawComment, 0, createSrcFile("package demo;", SAMPLE_PATH));

            // Then
            assertThat(optOutMode).isEqualTo(JHarmonizerOptOutMode.SORTING_OFF);
        }

        @Test
        void parseFileScopeOptOutMode_unknownToken_returnsNull() {
            // Given
            String rawComment = "// @jharmonizer:unknown-token";

            // When
            JHarmonizerOptOutMode optOutMode = JHarmonizerOptOutCommentUtilities.parseFileScopeOptOutMode(
                    rawComment, 0, createSrcFile("package demo;", SAMPLE_PATH));

            // Then
            assertThat(optOutMode).isNull();
        }

        @Test
        void parseFileScopeOptOutMode_withSrcOffsetAfterNewline_returnsMode() {
            // Given
            String srcCode = "package demo;\n// @jharmonizer:fully-off";
            String rawComment = "// @jharmonizer:fully-off";
            int commentOffset = "package demo;\n".length();

            // When
            JHarmonizerOptOutMode optOutMode = JHarmonizerOptOutCommentUtilities.parseFileScopeOptOutMode(
                    rawComment, commentOffset, createSrcFile(srcCode, SAMPLE_PATH));

            // Then
            assertThat(optOutMode).isEqualTo(JHarmonizerOptOutMode.FULLY_OFF);
        }
    }

    @Nested
    class CollectRawCommentsByRegex {

        @Test
        void collectRawCommentsByRegex_emptySource_returnsEmptyList() {
            // When
            List<JHarmonizerOptOutCommentUtilities.RawCommentMatch> rawCommentMatches =
                    JHarmonizerOptOutCommentUtilities.collectRawCommentsByRegex("");

            // Then
            assertThat(rawCommentMatches).isEmpty();
        }

        @Test
        void collectRawCommentsByRegex_sourceWithNoComments_returnsEmptyList() {
            // When
            List<JHarmonizerOptOutCommentUtilities.RawCommentMatch> rawCommentMatches =
                    JHarmonizerOptOutCommentUtilities.collectRawCommentsByRegex("class Sample {}");

            // Then
            assertThat(rawCommentMatches).isEmpty();
        }

        @Test
        void collectRawCommentsByRegex_singleLineComment_returnsMatch() {
            // When
            List<JHarmonizerOptOutCommentUtilities.RawCommentMatch> rawCommentMatches =
                    JHarmonizerOptOutCommentUtilities.collectRawCommentsByRegex("// my comment\nclass A {}");

            // Then
            assertThat(rawCommentMatches).hasSize(1);
            assertThat(rawCommentMatches.getFirst().getRawComment()).startsWith("//");
            assertThat(rawCommentMatches.getFirst().getCommentOffset()).isEqualTo(0);
        }

        @Test
        void collectRawCommentsByRegex_blockComment_returnsMatch() {
            // When
            List<JHarmonizerOptOutCommentUtilities.RawCommentMatch> rawCommentMatches =
                    JHarmonizerOptOutCommentUtilities.collectRawCommentsByRegex("/* block comment */ class A {}");

            // Then
            assertThat(rawCommentMatches).hasSize(1);
            assertThat(rawCommentMatches.getFirst().getRawComment()).startsWith("/*");
        }

        @Test
        void collectRawCommentsByRegex_multipleComments_returnsAllMatches() {
            // Given
            String srcCode = "// first\nclass A { /* second */ int x; // third\n}";

            // When
            List<JHarmonizerOptOutCommentUtilities.RawCommentMatch> rawCommentMatches =
                    JHarmonizerOptOutCommentUtilities.collectRawCommentsByRegex(srcCode);

            // Then
            assertThat(rawCommentMatches).hasSize(3);
        }
    }

    @Nested
    class FormatLocation {

        @Test
        void formatLocation_zeroOffset_returnsLineOneColOne() {
            // Given
            String srcCode = "class Sample {}";

            // When
            String formattedLocation =
                    JHarmonizerOptOutCommentUtilities.formatLocation(createSrcFile(srcCode, SAMPLE_PATH), 0);

            // Then
            assertThat(formattedLocation).isEqualTo("Sample.java:1:1");
        }

        @Test
        void formatLocation_afterFirstNewline_returnsLineTwo() {
            // Given
            String srcCode = "package demo;\nclass Sample {}";

            // When
            String formattedLocation =
                    JHarmonizerOptOutCommentUtilities.formatLocation(createSrcFile(srcCode, SAMPLE_PATH), 14);

            // Then
            assertThat(formattedLocation).startsWith("Sample.java:2:");
        }

        @Test
        void formatLocation_offsetPastEnd_returnsFormattedLocation() {
            // Given
            String srcCode = "class A {}";

            // When
            String formattedLocation =
                    JHarmonizerOptOutCommentUtilities.formatLocation(createSrcFile(srcCode, SAMPLE_PATH), 999);

            // Then
            assertThat(formattedLocation).startsWith("Sample.java:");
        }
    }

    @Nested
    class LogIgnoredOptOut {

        @Test
        void logIgnoredFileOptOutAtLocation_validLocation_doesNotThrow() {
            // When / Then
            JHarmonizerOptOutCommentUtilities.logIgnoredFileOptOutAtLocation(
                    "Sample.java:1:1", "Malformed opt-out comment is ignored");
        }
    }

    private static CtComment createInlineComment(String content) {
        return createComment(content, CommentType.INLINE);
    }

    private static CtComment createComment(String content, CommentType commentType) {
        Factory factory = new FactoryImpl(new DefaultCoreFactory(), new StandardEnvironment());
        CtComment comment = factory.Core().createComment();
        comment.setCommentType(commentType);
        comment.setContent(content);
        return comment;
    }
}
