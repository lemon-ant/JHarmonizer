package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import spoon.reflect.declaration.CtTypeMember;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class EnumMemberStartCorrectionResolver {

    @NonNull
    static Map<CtTypeMember, Integer> resolveCorrectedStarts(
            @NonNull String srcCode, @NonNull List<CtTypeMember> explicitTypeMembers) {
        CtTypeMember firstMember = explicitTypeMembers.stream()
                .min(java.util.Comparator.comparingInt(
                        member -> member.getPosition().getSourceStart()))
                .orElse(null);
        if (firstMember == null) {
            return Collections.emptyMap();
        }

        int srcStart = firstMember.getPosition().getSourceStart();
        int srcEnd = firstMember.getPosition().getSourceEnd();
        if (srcStart < 0 || srcEnd < srcStart || srcEnd >= srcCode.length()) {
            return Collections.emptyMap();
        }

        String fragment = srcCode.substring(srcStart, srcEnd + 1);
        String firstLine = firstMember.toString().lines().findFirst().orElse("").trim();
        if (firstLine.isEmpty()) {
            return Collections.emptyMap();
        }

        Pattern pattern = Pattern.compile(buildFirstLinePattern(firstLine));
        Matcher matcher = pattern.matcher(fragment);
        if (!matcher.find() || matcher.start() == 0) {
            return Collections.emptyMap();
        }

        Map<CtTypeMember, Integer> corrected = new LinkedHashMap<>();
        corrected.put(firstMember, srcStart + matcher.start());
        return Collections.unmodifiableMap(corrected);
    }

    @NonNull
    private static String buildFirstLinePattern(@NonNull String line) {
        StringBuilder patternBuilder = new StringBuilder();
        char[] chars = line.toCharArray();
        for (int index = 0; index < chars.length; index++) {
            char current = chars[index];
            if (Character.isWhitespace(current)) {
                char previous = findPreviousNonWhitespace(chars, index - 1);
                char next = findNextNonWhitespace(chars, index + 1);
                if (Character.isLetterOrDigit(previous) && Character.isLetterOrDigit(next)) {
                    patternBuilder.append("\\\\s+");
                } else {
                    patternBuilder.append("\\\\s*");
                }
                while (index + 1 < chars.length && Character.isWhitespace(chars[index + 1])) {
                    index++;
                }
            } else {
                patternBuilder.append(Pattern.quote(String.valueOf(current)));
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
