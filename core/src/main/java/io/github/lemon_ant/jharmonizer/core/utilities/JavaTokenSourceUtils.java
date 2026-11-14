// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.utilities;

import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findLineEnd;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findTrailingLineSeparatorsStart;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;

/**
 * Measures original Java token spellings in UTF-16 code units and reads lexical text blocks independently of Spoon
 * or annotation syntax. The measured value is source length, not the number of tokens.
 * Accepts complete token ranges already validated by JDT, not arbitrary source slices. Unicode translation preserves
 * original offsets; Java string escapes remain lexical. Text-block indentation is normalized with
 * {@link String#stripIndent()}.
 */
@UtilityClass
public class JavaTokenSourceUtils {

    /**
     * Measures one token's original spelling in UTF-16 code units, excluding comment terminators and incidental
     * text-block whitespace. Summing these lengths excludes gaps between tokens: {@code @Alpha ( 123 )} spans 14
     * source units, while its five token lengths sum to {@code 1 + 5 + 1 + 3 + 1 = 11}.
     * @param srcCode original source already validated lexically by JDT
     * @param start first original token offset
     * @param endExclusive first offset after the original token
     * @param token JDT public token identifier
     * @return measured source length in UTF-16 code units, or zero for EOF
     */
    public static int measureTokenSrcLength(@NonNull String srcCode, int start, int endExclusive, int token) {
        if (token == ITerminalSymbols.TokenNameEOF) {
            return 0;
        }
        if (token == ITerminalSymbols.TokenNameCOMMENT_LINE) {
            return measureLineCommentSrcLength(srcCode, start, endExclusive);
        }
        if (token == ITerminalSymbols.TokenNameTextBlock) {
            return measureTextBlockSrcLength(translateTokenSource(srcCode, start, endExclusive));
        }
        return endExclusive - start;
    }

    /**
     * Reads a text block as translated lexical tokens with its delimiters and without incidental indentation.
     * @param srcCode original source already validated lexically by JDT
     * @param start first offset of the opening delimiter
     * @param endExclusive first offset after the closing delimiter
     * @return token text without evaluating Java string escapes
     */
    @NonNull
    public static String readTextBlockToken(@NonNull String srcCode, int start, int endExclusive) {
        // JDT 3.46 can return the evaluated text-block value from getCurrentTokenSource(), omitting delimiters
        // and interpreting Java escapes. Read the original range to retain lexical delimiters and escape spellings.
        String translatedText =
                translateTokenSource(srcCode, start, endExclusive).getTranslatedText();
        return "\"\"\""
                + translatedText.substring(3, translatedText.length() - 3).stripIndent() + "\"\"\"";
    }

    /** Measures comment text in original UTF-16 units, excluding physical or Unicode-escaped line terminators. */
    private static int measureLineCommentSrcLength(String srcCode, int start, int endExclusive) {
        int backslashOffset = start;
        while (backslashOffset < endExclusive && srcCode.charAt(backslashOffset) != '\\') {
            backslashOffset++;
        }
        // Java string notation: "// note\n" has raw length 8; "// note\\u000a" and "// note\\u000d" have length 13.
        // Each measures 7. "// note\\u000d\\u000a" has raw length 19 and also measures 7, excluding both escapes.
        if (backslashOffset == endExclusive) {
            // No backslashes means Unicode translation leaves this token unchanged; its original offsets suffice.
            // Trim the physical CR/LF suffix directly in the original source without translating or mapping offsets.
            return findTrailingLineSeparatorsStart(start, endExclusive, srcCode) - start;
        }
        TranslatedTokenSource translatedTokenSource = translateTokenSource(srcCode, start, endExclusive);
        String translatedText = translatedTokenSource.getTranslatedText();
        int lineEnd = findLineEnd(0, translatedText.length(), translatedText);
        // Measure the original spelling, retaining Unicode escapes inside the comment up to the translated line end.
        // Java string notation: "// \\u0061\\u000a" has lineEnd 4 (after "// a"), mapped to source offset start + 9.
        // The measured length is 9 (3 + 6), retaining backslash-u0061 and excluding the terminating backslash-u000a.
        return translatedTokenSource.getSrcOffsets()[lineEnd] - start;
    }

    /** Measures original text-block spellings after incidental-whitespace and line-ending normalization. */
    private static int measureTextBlockSrcLength(TranslatedTokenSource tokenSource) {
        String translatedText = tokenSource.getTranslatedText();
        int[] srcOffsets = tokenSource.getSrcOffsets();
        int bodyEnd = translatedText.length() - 3;
        String body = translatedText.substring(3, bodyEnd);
        // Java string notation: "\"\"\"\n  a\n  \"\"\"" has raw length 13 and normalized length 3+1+1+1+3 = 9.
        // Spelling a as backslash-u0061 contributes six original units instead of one, yielding measured length 14.
        // Split CRLF, CR or LF, retaining the final empty line so closing-delimiter indentation participates.
        String[] originalLines = body.split("\\r\\n|\\r|\\n", -1);
        String[] normalizedLines = body.stripIndent().split("\n", -1);
        // Count both delimiters using their original spellings, including escaped quotes.
        int length = srcOffsets[3] - srcOffsets[0] + srcOffsets[translatedText.length()] - srcOffsets[bodyEnd];
        int lineStart = 3;
        for (int lineIndex = 0; lineIndex < originalLines.length; lineIndex++) {
            String originalLine = originalLines[lineIndex];
            int retainedEnd = originalLine.stripTrailing().length();
            int retainedStart = retainedEnd - normalizedLines[lineIndex].length();
            // A line spelled two spaces then backslash-u0061 retains all six escape characters after stripping
            // indentation. Mapping both slice boundaries also removes complete escaped spaces or tabs.
            length += srcOffsets[lineStart + retainedEnd] - srcOffsets[lineStart + retainedStart];
            int separatorStart = lineStart + originalLine.length();
            if (separatorStart < bodyEnd) {
                // stripIndent normalizes CRLF to LF; retain the final separator character's original range.
                if (translatedText.charAt(separatorStart) == '\r'
                        && separatorStart + 1 < bodyEnd
                        && translatedText.charAt(separatorStart + 1) == '\n') {
                    separatorStart++;
                }
                length += srcOffsets[separatorStart + 1] - srcOffsets[separatorStart];
                lineStart = separatorStart + 1;
            }
        }
        return length;
    }

    @NonNull
    private static TranslatedTokenSource translateTokenSource(String srcCode, int start, int endExclusive) {
        // JDT exposes translated token text without a per-character map back to the original source.
        // String.translateEscapes handles Java string escapes, not Unicode translation or original offsets.
        // Preserve the JLS 3.3 eligibility rules while recording each translated character's original start.
        StringBuilder translatedText = new StringBuilder(endExclusive - start);
        int[] srcOffsets = new int[endExclusive - start + 1];
        int srcOffset = start;
        int consecutiveBackslashes = 0;
        boolean precedingUnicodeEscape = false;
        while (srcOffset < endExclusive) {
            srcOffsets[translatedText.length()] = srcOffset;
            char character = srcCode.charAt(srcOffset);
            srcOffset++;
            // JLS 3.3: an eligible backslash follows either a translated escape or an even backslash run.
            // Two raw backslashes before u0041 remain literal; a backslash produced by an escape is not recursive.
            // Java string notation: "\\u0061" -> "a"; "\\\\u0061" -> "\\\\u0061";
            // "\\u005c\\u0061" -> "\\a", but "\\u005cu0061" -> "\\u0061" without another translation pass.
            if (character == '\\'
                    && (precedingUnicodeEscape || consecutiveBackslashes % 2 == 0)
                    && srcOffset < endExclusive
                    && srcCode.charAt(srcOffset) == 'u') {
                // A Unicode marker permits multiple 'u' characters. JDT has validated the complete token, so
                // at least four hexadecimal digits follow this run before endExclusive; no extra bounds guard.
                while (srcCode.charAt(srcOffset) == 'u') {
                    srcOffset++;
                }
                character = (char) Integer.parseInt(srcCode, srcOffset, srcOffset + 4, 16);
                srcOffset += 4;
                precedingUnicodeEscape = true;
            } else {
                precedingUnicodeEscape = false;
            }
            translatedText.append(character);
            consecutiveBackslashes = character == '\\' ? consecutiveBackslashes + 1 : 0;
        }
        srcOffsets[translatedText.length()] = endExclusive;
        return new TranslatedTokenSource(srcOffsets, translatedText.toString());
    }

    /** Original offsets for each translated character and the exclusive end of the token. */
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    private static class TranslatedTokenSource {
        int[] srcOffsets;

        @NonNull
        String translatedText;
    }
}
