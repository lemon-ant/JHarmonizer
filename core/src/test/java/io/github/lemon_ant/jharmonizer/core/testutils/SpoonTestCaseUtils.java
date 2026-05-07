// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.testutils;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static java.util.Objects.requireNonNull;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

@UtilityClass
public class SpoonTestCaseUtils {
    private static final PrinterConfig DEFAULT_PRINTER_CONFIG = new PrinterConfig(true, true, false);

    public static SpoonAstModel parseAstModelFromJavaFixtureResource(URL javaFixtureResource) {
        requireNonNull(javaFixtureResource, "javaFixtureResource cannot be null");

        String srcCode = TestCaseResourceUtils.readClasspathResourceAsString(javaFixtureResource);
        return SpoonParser.parseJavaSrcFile(
                createSrcFile(srcCode, Path.of(extractFileNameWithExtension(javaFixtureResource))),
                DEFAULT_PRINTER_CONFIG);
    }

    public static CtType<?> parseMainTypeFromJavaFixtureResource(URL javaFixtureResource) {
        SpoonAstModel spoonAstModel = parseAstModelFromJavaFixtureResource(javaFixtureResource);
        return spoonAstModel
                .getMainType()
                .orElseThrow(() -> new IllegalStateException(
                        "Expected a main type to be detected for fixture URL: " + javaFixtureResource));
    }

    public static MemberDescriptor requireMemberDescriptorByName(
            Map<CtTypeMember, MemberDescriptor> describedMembers, String expectedName) {
        requireNonNull(describedMembers, "describedMembers cannot be null");
        requireNonNull(expectedName, "expectedName cannot be null");

        return describedMembers.values().stream()
                .filter(memberDescriptor ->
                        memberDescriptor.getName().filter(expectedName::equals).isPresent())
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No member descriptor found for name: %s. Available named members: %s"
                                .formatted(
                                        expectedName,
                                        describedMembers.values().stream()
                                                .flatMap(memberDescriptor -> memberDescriptor.getName().stream())
                                                .sorted()
                                                .toList())));
    }

    @NonNull
    public static CtTypeMember requireTypeMemberBySimpleName(
            @NonNull Collection<CtTypeMember> typeMembers, @NonNull String expectedSimpleName) {
        return typeMembers.stream()
                .filter(typeMember -> expectedSimpleName.equals(typeMember.getSimpleName()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No type member found for simple name: %s. Available members: %s"
                                .formatted(
                                        expectedSimpleName,
                                        typeMembers.stream()
                                                .map(CtTypeMember::getSimpleName)
                                                .sorted()
                                                .toList())));
    }

    @NonNull
    public static CtTypeMember requireTypeMemberBySimpleName(
            Map<CtTypeMember, ?> typeMembers, String expectedSimpleName) {
        return requireTypeMemberBySimpleName(typeMembers.keySet(), expectedSimpleName);
    }

    public static MemberDescriptor requireUniqueMemberDescriptorByKind(
            Map<CtTypeMember, MemberDescriptor> describedMembers, MemberKind expectedKind) {
        requireNonNull(describedMembers, "describedMembers cannot be null");
        requireNonNull(expectedKind, "expectedKind cannot be null");

        List<MemberDescriptor> matchingDescriptors = describedMembers.values().stream()
                .filter(memberDescriptor -> memberDescriptor.getMemberKind() == expectedKind)
                .sorted(Comparator.comparing(
                        memberDescriptor -> memberDescriptor.getName().orElse("<unnamed>")))
                .toList();

        if (matchingDescriptors.size() != 1) {
            throw new IllegalStateException("Expected exactly one descriptor with kind %s, but found: %s"
                    .formatted(expectedKind, matchingDescriptors));
        }

        return matchingDescriptors.getFirst();
    }

    /**
     * Extracts the last path segment from the given URL, e.g. "File.java".
     * <p>
     * Notes:
     * - Uses {@link URL#getPath()} to avoid parsing full external form.
     * - Does not URL-decode (%20 etc.). If you need decoding, use a URI-based variant.
     */
    @NonNull
    private static String extractFileNameWithExtension(URL resourceUrl) {
        String urlPath = resourceUrl.getPath();
        if (urlPath == null || urlPath.isEmpty()) {
            throw new IllegalArgumentException("URL has no path: " + resourceUrl);
        }

        int lastSlashIndex = urlPath.lastIndexOf('/');
        int fileNameStartIndex = lastSlashIndex + 1;

        if (fileNameStartIndex >= urlPath.length()) {
            throw new IllegalArgumentException("URL path ends with '/': " + resourceUrl);
        }

        return urlPath.substring(fileNameStartIndex);
    }
}
