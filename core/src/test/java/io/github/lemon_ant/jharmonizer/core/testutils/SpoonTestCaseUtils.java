package io.github.lemon_ant.jharmonizer.core.testutils;

import static java.util.Objects.requireNonNull;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.net.URL;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/** Test-only utilities for Spoon-based fixtures. */
@UtilityClass
public final class SpoonTestCaseUtils {

    //TODO Refactor
    public static CtType<?> parseMainTypeFromJavaFixtureResource(URL javaFixtureResource, Path pseudoSourcePath) {
        requireNonNull(javaFixtureResource, "javaFixtureResource cannot be null");
        requireNonNull(pseudoSourcePath, "pseudoSourcePath cannot be null");

        String sourceCode = TestCaseResourceUtils.readClasspathResourceAsString(javaFixtureResource);
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(pseudoSourcePath, sourceCode);
        return spoonAstModel
                .getMainType()
                .orElseThrow(() -> new IllegalStateException(
                        "Expected a main type to be detected for fixture URL: " + javaFixtureResource));
    }

    public static CtTypeMember requireTypeMemberBySimpleName(
            Map<CtTypeMember, MemberDescriptor> describedMembers, String expectedSimpleName) {
        requireNonNull(describedMembers, "describedMembers cannot be null");
        requireNonNull(expectedSimpleName, "expectedSimpleName cannot be null");

        List<CtTypeMember> matchingMembers = describedMembers.keySet().stream()
                .filter(typeMember -> expectedSimpleName.equals(typeMember.getSimpleName()))
                .sorted(Comparator.comparing(CtTypeMember::getSimpleName))
                .toList();

        if (matchingMembers.isEmpty()) {
            throw new IllegalStateException(
                    "No CtTypeMember found for simple name: '%s'. Available simple names: %s"
                            .formatted(
                                    expectedSimpleName,
                                    describedMembers.keySet().stream()
                                            .map(CtTypeMember::getSimpleName)
                                            .sorted()
                                            .toList()));
        }

        if (matchingMembers.size() != 1) {
            throw new IllegalStateException(
                    "Expected exactly one CtTypeMember with simple name '%s', but found %s: %s"
                            .formatted(expectedSimpleName, matchingMembers.size(), matchingMembers));
        }

        return matchingMembers.getFirst();
    }

    public static MemberDescriptor requireMemberDescriptorByName(
            Map<CtTypeMember, MemberDescriptor> describedMembers, String expectedName) {
        requireNonNull(describedMembers, "describedMembers cannot be null");
        requireNonNull(expectedName, "expectedName cannot be null");

        List<MemberDescriptor> matchingDescriptors = describedMembers.values().stream()
                .filter(memberDescriptor -> memberDescriptor.getName().stream().anyMatch(expectedName::equals))
                .sorted(Comparator.comparing(memberDescriptor -> memberDescriptor.getName().orElse("<unnamed>")))
                .toList();

        if (matchingDescriptors.isEmpty()) {
            throw new IllegalStateException(
                    "No MemberDescriptor found for name: '%s'. Available named members: %s"
                            .formatted(
                                    expectedName,
                                    describedMembers.values().stream()
                                            .flatMap(memberDescriptor -> memberDescriptor.getName().stream())
                                            .sorted()
                                            .toList()));
        }

        if (matchingDescriptors.size() != 1) {
            throw new IllegalStateException(
                    "Expected exactly one MemberDescriptor with name '%s', but found %s: %s"
                            .formatted(expectedName, matchingDescriptors.size(), matchingDescriptors));
        }

        return matchingDescriptors.getFirst();
    }

    public static MemberDescriptor requireUniqueMemberDescriptorByKind(
            Map<CtTypeMember, MemberDescriptor> describedMembers, MemberKind expectedKind) {
        requireNonNull(describedMembers, "describedMembers cannot be null");
        requireNonNull(expectedKind, "expectedKind cannot be null");

        List<MemberDescriptor> matchingDescriptors = describedMembers.values().stream()
                .filter(memberDescriptor -> memberDescriptor.getMemberKind() == expectedKind)
                .sorted(Comparator.comparing(memberDescriptor -> memberDescriptor.getName().orElse("<unnamed>")))
                .toList();

        if (matchingDescriptors.size() != 1) {
            throw new IllegalStateException(
                    "Expected exactly one descriptor with kind %s, but found: %s"
                            .formatted(expectedKind, matchingDescriptors));
        }

        return matchingDescriptors.getFirst();
    }
}
