package io.github.lemon_ant.jharmonizer.core.optout;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtComment.CommentType;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.visitor.filter.TypeFilter;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class JHarmonizerOptOutResolver {
    // Matches exactly two Java comment families in raw source:
    // 1) /\* ... *\/   (including multiline block comments)
    // 2) // ...        (single-line comments up to newline/end-of-file)
    //
    // Pattern breakdown:
    // - (?s) enables DOTALL so ".*?" can cross line breaks inside block comments.
    // - /\\*.*?\\*/ is a non-greedy block comment matcher (first closing */ wins).
    // - | alternates with single-line comments.
    // - //.*?(?:\\R|$) captures line comments and stops at a line-break or EOF.
    //
    // We intentionally do not parse Java syntax here; this lexer-like pattern is only used for
    // file-scope opt-out probing in package-declaration units where Spoon comment attachment is unreliable.
    private static final Pattern COMMENT_PATTERN = Pattern.compile("(?s)/\\*.*?\\*/|//.*?(?:\\R|$)");

    // TODO Track/confirm Spoon issue for package-info file-scope comments and replace this workaround
    //  with pure AST-based resolution once upstream behavior is fixed (issue link should be added here).

    // TODO rethink these fields and make the class static and stateless
    @NonNull
    private final SrcFile srcFile;

    @NonNull
    private final CtCompilationUnit compilationUnit;

    /**
     * Resolves file-level and type-level opt-out directives from a parsed compilation unit.
     *
     * @param srcFile the source file that owns the parsed compilation unit
     * @param compilationUnit the parsed compilation unit to inspect
     * @return the resolved opt-out summary
     */
    @NonNull
    public static JHarmonizerOptOuts resolve(@NonNull SrcFile srcFile, @NonNull CtCompilationUnit compilationUnit) {
        return new JHarmonizerOptOutResolver(srcFile, compilationUnit).resolve();
    }

    @NonNull
    private JHarmonizerOptOuts resolve() {
        JHarmonizerOptOutMode fileOptOutMode = resolveFileOptOutMode();
        if (fileOptOutMode == JHarmonizerOptOutMode.FULLY_OFF) {
            return new JHarmonizerOptOuts(JHarmonizerOptOutMode.FULLY_OFF, Map.of());
        }

        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtType<?>, JHarmonizerOptOutMode> typeOptOutModes = new HashMap<>();
        boolean sortingDisabledInParents = fileOptOutMode == JHarmonizerOptOutMode.SORTING_OFF;
        for (CtType<?> declaredType : compilationUnit.getDeclaredTypes()) {
            collectTypeOptOutModes(declaredType, sortingDisabledInParents, typeOptOutModes);
        }

        return typeOptOutModes.isEmpty() && fileOptOutMode == null
                ? JHarmonizerOptOuts.empty()
                : new JHarmonizerOptOuts(fileOptOutMode, typeOptOutModes);
    }

    @Nullable
    private JHarmonizerOptOutMode resolveFileOptOutMode() {
        if (!isPackageDeclarationUnit()) {
            return resolveFileOptOutModeFromAstComments();
        }

        return resolveFileOptOutModeFromRawSourceForPackageDeclaration();
    }

    @Nullable
    private JHarmonizerOptOutMode resolveFileOptOutModeFromAstComments() {
        List<CtComment> fileComments = compilationUnit.getElements(new TypeFilter<>(CtComment.class)).stream()
                .filter(JHarmonizerOptOutResolver::isStandaloneComment)
                .filter(this::isBeforeFirstDeclaredType)
                .sorted(Comparator.comparingInt(comment -> comment.getPosition().getSourceStart()))
                .toList();
        JHarmonizerOptOutMode fileOptOutMode = null;
        CtComment previousFileComment = null;
        for (CtComment fileComment : fileComments) {
            JHarmonizerOptOutMode currentMode = parseOptOutMode(fileComment);
            if (currentMode == null) {
                continue;
            }
            if (fileOptOutMode != null) {
                logIgnoredOptOut(
                        fileComment,
                        "Later file-scope opt-out replaces the previously parsed one from %s; the last applicable"
                                        .formatted(formatLocation(previousFileComment.getPosition()))
                                + " file-scope opt-out wins");
            }

            fileOptOutMode = currentMode;
            previousFileComment = fileComment;
            if (fileOptOutMode == JHarmonizerOptOutMode.FULLY_OFF) {
                break;
            }
        }
        return fileOptOutMode;
    }

    @Nullable
    JHarmonizerOptOutMode resolveFileOptOutModeFromRawSourceForPackageDeclaration() {
        String fileScopeSource = srcFile.getSrcCode().substring(0, findFileScopeEndExclusive());
        JHarmonizerOptOutMode fileOptOutMode = null;
        Integer previousCommentOffset = null;
        for (RawCommentMatch rawCommentMatch : collectRawFileScopeComments(fileScopeSource)) {
            String rawComment = rawCommentMatch.getRawComment();
            int commentOffset = rawCommentMatch.getCommentOffset();
            JHarmonizerOptOutMode currentMode = parseOptOutMode(rawComment, commentOffset);
            if (currentMode == null) {
                continue;
            }
            if (fileOptOutMode != null) {
                logIgnoredOptOut(
                        commentOffset,
                        "Later file-scope opt-out replaces the previously parsed one from %s; the last applicable"
                                        .formatted(formatFileLineAndColumnLocation(previousCommentOffset))
                                + " file-scope opt-out wins");
            }

            fileOptOutMode = currentMode;
            previousCommentOffset = commentOffset;
            if (fileOptOutMode == JHarmonizerOptOutMode.FULLY_OFF) {
                break;
            }
        }
        return fileOptOutMode;
    }

    private void collectTypeOptOutModes(
            CtType<?> currentType,
            boolean sortingDisabledInParents,
            Map<CtType<?>, JHarmonizerOptOutMode> typeOptOutModes) {
        JHarmonizerOptOutMode directOptOutMode = findTypeOptOutMode(currentType);
        if (directOptOutMode == JHarmonizerOptOutMode.FULLY_OFF) {
            typeOptOutModes.put(currentType, JHarmonizerOptOutMode.FULLY_OFF);
            return;
        }

        boolean sortingDisabledForCurrentType = sortingDisabledInParents;
        if (directOptOutMode == JHarmonizerOptOutMode.SORTING_OFF && !sortingDisabledInParents) {
            typeOptOutModes.put(currentType, JHarmonizerOptOutMode.SORTING_OFF);
            sortingDisabledForCurrentType = true;
        }

        for (CtType<?> nestedType : currentType.getNestedTypes()) {
            collectTypeOptOutModes(nestedType, sortingDisabledForCurrentType, typeOptOutModes);
        }
    }

    @Nullable
    private JHarmonizerOptOutMode findTypeOptOutMode(CtType<?> currentType) {
        List<CtComment> leadingTypeComments = currentType.getComments().stream()
                .filter(comment -> comment.getPosition().getEndLine()
                        < currentType.getPosition().getLine())
                .sorted(Comparator.comparingInt(comment -> comment.getPosition().getSourceStart()))
                .toList();
        JHarmonizerOptOutMode typeOptOutMode = null;
        CtComment previousTypeComment = null;
        for (CtComment leadingTypeComment : leadingTypeComments) {
            JHarmonizerOptOutMode currentMode = parseOptOutMode(leadingTypeComment);
            if (currentMode == null) {
                continue;
            }
            if (typeOptOutMode != null) {
                logIgnoredOptOut(
                        leadingTypeComment,
                        "Later opt-out comment for type '%s' replaces the previously parsed one from %s; the last"
                                        .formatted(
                                                currentType.getQualifiedName(),
                                                formatLocation(previousTypeComment.getPosition()))
                                + " applicable type-level opt-out wins");
            }
            typeOptOutMode = currentMode;
            previousTypeComment = leadingTypeComment;
            if (typeOptOutMode == JHarmonizerOptOutMode.FULLY_OFF) {
                break;
            }
        }
        return typeOptOutMode;
    }

    @Nullable
    private JHarmonizerOptOutMode parseOptOutMode(CtComment comment) {
        String normalizedContent = comment.getContent().trim().toLowerCase(Locale.ROOT);
        int tokenPrefixIndex = normalizedContent.indexOf(JHarmonizerOptOutMode.TOKEN_PREFIX);
        if (tokenPrefixIndex < 0) {
            return null;
        }
        if (comment.getCommentType() == CommentType.JAVADOC) {
            logIgnoredOptOut(comment, "Javadoc opt-out comments are ignored");
            return null;
        }
        if (tokenPrefixIndex != 0) {
            logIgnoredOptOut(comment, "Malformed opt-out comment is ignored");
            return null;
        }

        try {
            return JHarmonizerOptOutMode.fromToken(normalizedContent);
        } catch (IllegalArgumentException exception) {
            logIgnoredOptOut(comment, exception.getMessage());
            return null;
        }
    }

    private int findFileScopeEndExclusive() {
        return compilationUnit.getDeclaredTypes().stream()
                .map(CtType::getPosition)
                .mapToInt(SourcePosition::getSourceStart)
                .min()
                .orElse(srcFile.getSrcCode().length());
    }

    private boolean isPackageDeclarationUnit() {
        return compilationUnit.getUnitType() == CtCompilationUnit.UNIT_TYPE.PACKAGE_DECLARATION;
    }

    /**
     * Extracts raw Java comments from file-scope source text in encounter order.
     *
     * <p>This helper is package-private intentionally to enable focused tests for the regex behavior and
     * comment tokenization edge cases without forcing full Spoon parsing in every combinatorial case.
     *
     * @param fileScopeSource source fragment that contains only file-scope text
     * @return ordered raw comment matches with their source offsets relative to the provided fragment
     */
    @NonNull
    static List<RawCommentMatch> collectRawFileScopeComments(@NonNull String fileScopeSource) {
        Matcher commentMatcher = COMMENT_PATTERN.matcher(fileScopeSource);
        List<RawCommentMatch> matches = new ArrayList<>();
        while (commentMatcher.find()) {
            matches.add(new RawCommentMatch(commentMatcher.group(), commentMatcher.start()));
        }
        return List.copyOf(matches);
    }

    @Nullable
    private JHarmonizerOptOutMode parseOptOutMode(String rawComment, int commentOffset) {
        String normalizedContent = rawComment
                .replaceFirst("^//", "")
                .replaceFirst("^/\\*+", "")
                .replaceFirst("\\*+/$", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        int tokenPrefixIndex = normalizedContent.indexOf(JHarmonizerOptOutMode.TOKEN_PREFIX);
        if (tokenPrefixIndex < 0) {
            return null;
        }
        if (rawComment.startsWith("/**")) {
            logIgnoredOptOut(commentOffset, "Javadoc opt-out comments are ignored");
            return null;
        }
        if (tokenPrefixIndex != 0) {
            logIgnoredOptOut(commentOffset, "Malformed opt-out comment is ignored");
            return null;
        }

        try {
            return JHarmonizerOptOutMode.fromToken(normalizedContent);
        } catch (IllegalArgumentException exception) {
            logIgnoredOptOut(commentOffset, exception.getMessage());
            return null;
        }
    }

    private void logIgnoredOptOut(CtComment comment, String message) {
        if (log.isWarnEnabled()) {
            log.warn("{} at {}", message, formatLocation(comment.getPosition()));
        }
    }

    private void logIgnoredOptOut(int commentOffset, String message) {
        if (log.isWarnEnabled()) {
            log.warn("{} at {}", message, formatFileLineAndColumnLocation(commentOffset));
        }
    }

    @NonNull
    private String formatLocation(SourcePosition sourcePosition) {
        return srcFile.getPath() + ":" + sourcePosition.getLine() + ":" + sourcePosition.getColumn();
    }

    @NonNull
    private String formatFileLineAndColumnLocation(int sourceOffset) {
        int line = 1;
        int column = 1;
        String srcCode = srcFile.getSrcCode();
        for (int index = 0; index < sourceOffset && index < srcCode.length(); index++) {
            if (srcCode.charAt(index) == '\n') {
                line++;
                column = 1;
                continue;
            }
            column++;
        }
        return srcFile.getPath() + ":" + line + ":" + column;
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    static final class RawCommentMatch {
        @NonNull
        private final String rawComment;

        private final int commentOffset;
    }

    private static boolean isStandaloneComment(CtComment comment) {
        CtElement parent = comment.getParent();
        return !(parent instanceof CtTypeMember);
    }

    private boolean isBeforeFirstDeclaredType(CtComment comment) {
        return comment.getPosition().getSourceEnd()
                < compilationUnit.getDeclaredTypes().stream()
                        .map(CtType::getPosition)
                        .mapToInt(SourcePosition::getSourceStart)
                        .min()
                        .orElse(Integer.MAX_VALUE);
    }
}
