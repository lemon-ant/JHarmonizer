// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

@UtilityClass
final class EnumMemberStartCorrectionResolver {

    /**
     * Resolves corrected source-start positions for enum members whose Spoon-reported start is offset
     * into a preceding lambda body. Only the first enum member (by source position) is examined; if its
     * Spoon-reported start falls inside a lambda, the corrected position is returned in the map.
     *
     * @param srcCode             the full original source code of the compilation unit
     * @param explicitTypeMembers the list of explicitly declared enum type members (must not be empty)
     * @return a map from member to its corrected start index, or an empty map when no correction is needed
     */
    @NonNull
    static Map<CtTypeMember, Integer> resolveCorrectedStarts(
            @NonNull String srcCode, @NonNull List<CtTypeMember> explicitTypeMembers) {
        CtTypeMember firstMember = explicitTypeMembers.stream()
                .min(Comparator.comparingInt(member -> member.getPosition().getSourceStart()))
                .orElseThrow(() -> new IllegalStateException("Expected at least one explicit type member"));

        int srcStart = firstMember.getPosition().getSourceStart();
        int srcEnd = firstMember.getPosition().getSourceEnd();
        String memberSourceFragment = srcCode.substring(srcStart, srcEnd + 1);
        String firstLine = firstMember
                .toString()
                .lines()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed to find first line for enum member"))
                .trim();
        if (firstLine.isEmpty()) {
            throw new IllegalStateException("First line for enum member is blank");
        }

        Pattern pattern = Pattern.compile(buildFirstLinePattern(firstLine));
        Matcher matcher = pattern.matcher(memberSourceFragment);
        if (!matcher.find() || matcher.start() == 0) {
            return Collections.emptyMap();
        }
        return Map.of(firstMember, srcStart + matcher.start());
    }

    @NonNull
    private static String buildFirstLinePattern(@NonNull String line) {
        StringBuilder patternBuilder = new StringBuilder();
        char[] chars = line.toCharArray();
        int index = 0;
        while (index < chars.length) {
            char current = chars[index];
            if (Character.isWhitespace(current)) {
                char previous = findPreviousNonWhitespace(chars, index - 1);
                char next = findNextNonWhitespace(chars, index + 1);
                if (Character.isLetterOrDigit(previous) && Character.isLetterOrDigit(next)) {
                    patternBuilder.append("\\s+");
                } else {
                    patternBuilder.append("\\s*");
                }
                int nextIndex = index + 1;
                while (nextIndex < chars.length && Character.isWhitespace(chars[nextIndex])) {
                    nextIndex++;
                }
                index = nextIndex;
            } else {
                patternBuilder.append(Pattern.quote(String.valueOf(current)));
                index++;
            }
        }
        return patternBuilder.toString();
    }

    private static char findPreviousNonWhitespace(char[] chars, int index) {
        for (int cursor = index; cursor >= 0; cursor--) {
            if (!Character.isWhitespace(chars[cursor])) {
                return chars[cursor];
            }
        }
        return '\0';
    }

    private static char findNextNonWhitespace(char[] chars, int index) {
        for (int cursor = index; cursor < chars.length; cursor++) {
            if (!Character.isWhitespace(chars[cursor])) {
                return chars[cursor];
            }
        }
        return '\0';
    }
}
