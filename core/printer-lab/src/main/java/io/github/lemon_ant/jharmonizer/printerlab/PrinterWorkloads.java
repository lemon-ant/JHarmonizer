// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.printerlab;

import static lombok.AccessLevel.PRIVATE;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFilesHandler;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonSorter;
import io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils;
import io.github.lemon_ant.jharmonizer.core.translator.SerializedSrcWithSkippedTypeRanges;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.ToolProvider;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtType;

/** Prepares the same inputs and correctness checks for every printer implementation. */
@UtilityClass
final class PrinterWorkloads {
    @NonNull
    static final List<String> NAMES = List.of("fixtures", "flat128", "flat1024", "nested128");

    /**
     * Freezes and verifies real and generated printer workloads.
     * @param repository repository root
     * @param output run directory
     * @return frozen workload descriptors
     */
    @NonNull
    static List<Map<String, Object>> create(@NonNull Path repository, @NonNull Path output) throws Exception {
        Path corpus = output.resolve("corpus");
        for (String line : LabFiles.readManifest(repository.resolve("core/printer-lab/corpus.tsv"))) {
            String[] columns = line.split("\t");
            Path original = repository.resolve("core/src/test/resources").resolve(columns[1]);
            String separator =
                    switch (columns[2]) {
                        case "LF" -> "\n";
                        case "CRLF" -> "\r\n";
                        case "CR" -> "\r";
                        default -> throw new IllegalArgumentException("Unknown line ending: " + columns[2]);
                    };
            Path destination = corpus.resolve("fixtures").resolve(columns[0]).resolve(original.getFileName());
            Files.createDirectories(destination.getParent());
            Files.writeString(
                    destination,
                    Files.readString(original)
                            .replace("\r\n", "\n")
                            .replace("\r", "\n")
                            .replace("\n", separator));
        }
        writeSynthetic(corpus, "flat128", "Flat128", 128);
        writeSynthetic(corpus, "flat1024", "Flat1024", 1024);
        StringBuilder nestedMembers = new StringBuilder();
        for (int typeIndex = 127; typeIndex >= 0; typeIndex--) {
            nestedMembers.append("    static class Nested%03d {\n".formatted(typeIndex));
            nestedMembers.append(renderFields(8, "        "));
            nestedMembers.append("    }\n");
        }
        Path nested = corpus.resolve("nested128/Nested128.java");
        Files.createDirectories(nested.getParent());
        Files.writeString(nested, readTemplate("nested-template.java.txt").replace("${members}", nestedMembers));
        List<Map<String, Object>> workloads = new ArrayList<>();
        Map<String, Object> golden = new LinkedHashMap<>();
        for (String name : NAMES) {
            List<PreparedInput> inputs = prepare(corpus, name);
            verify(inputs);
            List<Map<String, Object>> documents = new ArrayList<>();
            for (PreparedInput input : inputs) {
                Path original = corpus.resolve(input.getId());
                String srcCode = Files.readString(original);
                Map<String, Object> expected = new LinkedHashMap<>();
                expected.put("sourceSha256", LabFiles.hash(srcCode));
                expected.put("output", input.getExpectedSrcCode());
                expected.put("outputSha256", LabFiles.hash(input.getExpectedSrcCode()));
                expected.put("ranges", input.getExpectedRanges());
                expected.put("modelSha256", input.getModelFingerprint());
                golden.put(input.getId(), Map.copyOf(expected));
                Path compiledSrc = output.resolve("compiled-output-src").resolve(input.getId());
                Path classes =
                        output.resolve("compiled-output").resolve(input.getId().replace(".java", ""));
                Files.createDirectories(compiledSrc.getParent());
                Files.createDirectories(classes);
                Files.writeString(compiledSrc, input.getExpectedSrcCode());
                int exitCode = ToolProvider.getSystemJavaCompiler()
                        .run(
                                null,
                                null,
                                null,
                                "--release",
                                "21",
                                "-proc:none",
                                "-d",
                                classes.toString(),
                                compiledSrc.toString());
                if (exitCode != 0) {
                    throw new IllegalStateException("Printed workload does not compile: " + input.getId());
                }
                documents.add(Map.of(
                        "id",
                        input.getId(),
                        "inputBytes",
                        srcCode.getBytes(StandardCharsets.UTF_8).length,
                        "inputLines",
                        srcCode.lines().count(),
                        "outputCharacters",
                        input.getExpectedSrcCode().length(),
                        "sourceSha256",
                        LabFiles.hash(srcCode),
                        "outputSha256",
                        LabFiles.hash(input.getExpectedSrcCode())));
            }
            workloads.add(
                    Map.of("name", name, "documentsPerOperation", inputs.size(), "documents", List.copyOf(documents)));
        }
        LabFiles.writeJson(corpus.resolve("golden.json"), Map.copyOf(golden));
        LabFiles.writeJson(output.resolve("golden.json"), Map.copyOf(golden));
        return List.copyOf(workloads);
    }

    /**
     * Prepares models outside the timed boundary.
     * @param corpus prepared source root
     * @param workload workload name
     * @return independent, sorted models
     */
    @NonNull
    static List<PreparedInput> prepare(@NonNull Path corpus, @NonNull String workload) throws Exception {
        SpoonSorter sorter = new SpoonSorter(ConfigurationManager.loadDefaultConfig());
        PrinterConfig config = new PrinterConfig(true, true, true);
        List<SrcFile> files;
        try (Stream<SrcFile> srcFiles =
                SrcFilesHandler.readJavaFiles(corpus.resolve(workload), List.of("**/*.java"), List.of())) {
            files = srcFiles.sorted(
                            Comparator.comparing(srcFile -> srcFile.getPath().toString()))
                    .toList();
        }
        if (files.isEmpty()) {
            throw new IllegalStateException("Empty workload: " + corpus.resolve(workload));
        }
        List<PreparedInput> inputs = new ArrayList<>();
        for (SrcFile srcFile : files) {
            SpoonAstModel model = SpoonParser.parseJavaSrcFile(srcFile, config);
            if (SpoonTypeUtils.hasNoDeclaredTypes(model.getCompilationUnit())) {
                throw new IllegalStateException("Workload bypasses the printer: " + srcFile.getPath());
            }
            sorter.sortCompilationUnitRecursively(
                    model.getCompilationUnit(), model.getOptOuts().getSortingSkippedTypes());
            String fingerprint = fingerprint(model);
            SerializedSrcWithSkippedTypeRanges printed =
                    model.getSerializedSrcCode().get();
            String id = corpus.relativize(srcFile.getPath()).toString().replace('\\', '/');
            inputs.add(
                    new PreparedInput(id, model, printed.getSerializedSrcCode(), describeRanges(printed), fingerprint));
        }
        Path golden = corpus.resolve("golden.json");
        if (Files.exists(golden)) {
            JsonNode expected = LabFiles.readJson(golden);
            for (PreparedInput input : inputs) {
                JsonNode document = expected.path(input.getId());
                if (!input.getExpectedSrcCode().equals(document.path("output").asText())
                        || !input.getModelFingerprint()
                                .equals(document.path("modelSha256").asText())
                        || !input.getExpectedRanges()
                                .equals(document.path("ranges").asText())) {
                    throw new IllegalStateException("Worker differs from frozen result: " + input.getId());
                }
            }
        }
        return List.copyOf(inputs);
    }

    /**
     * Checks stable output, skipped ranges and model structure.
     * @param inputs prepared models to check repeatedly without changing their state
     */
    static void verify(@NonNull List<PreparedInput> inputs) {
        for (PreparedInput input : inputs) {
            for (int repetition = 0; repetition < 10; repetition++) {
                SerializedSrcWithSkippedTypeRanges printed =
                        input.getModel().getSerializedSrcCode().get();
                if (!input.getExpectedSrcCode().equals(printed.getSerializedSrcCode())
                        || !input.getExpectedRanges().equals(describeRanges(printed))
                        || !input.getModelFingerprint().equals(fingerprint(input.getModel()))) {
                    throw new IllegalStateException("Serialization changed output, ranges or model: " + input.getId());
                }
            }
        }
    }

    @NonNull
    private static String describeRanges(SerializedSrcWithSkippedTypeRanges result) {
        return result.getSortingSkippedTypeRanges().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getQualifiedName()))
                .map(entry -> entry.getKey().getQualifiedName() + ":"
                        + entry.getValue().getStartInclusive() + ":"
                        + entry.getValue().getEndExclusive() + ":"
                        + LabFiles.hash(result.getSerializedSrcCode()
                                .substring(
                                        entry.getValue().getStartInclusive(),
                                        entry.getValue().getEndExclusive())))
                .collect(Collectors.joining("\n"));
    }

    @NonNull
    private static String fingerprint(SpoonAstModel model) {
        String structure = SpoonTypeUtils.getAllTypes(model.getCompilationUnit()).stream()
                .sorted(Comparator.comparing(CtType::getQualifiedName))
                .map(type -> type.getQualifiedName() + ":"
                        + type.getTypeMembers().stream()
                                .map(member -> member.getClass().getSimpleName() + ":" + member.getSimpleName() + ":"
                                        + (member.getPosition().isValidPosition()
                                                ? member.getPosition().getSourceStart()
                                                : -1)
                                        + ":"
                                        + (member.getPosition().isValidPosition()
                                                ? member.getPosition().getSourceEnd()
                                                : -1))
                                .collect(Collectors.joining("|")))
                .collect(Collectors.joining("\n"));
        return LabFiles.hash(structure);
    }

    @NonNull
    private static String readTemplate(String name) throws Exception {
        try (InputStream stream = PrinterWorkloads.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) {
                throw new IllegalStateException("Missing source template: " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    @NonNull
    private static String renderFields(int count, String indentation) {
        StringBuilder fields = new StringBuilder();
        for (int index = count - 1; index >= 0; index--) {
            fields.append(indentation).append("int field%04d = %d;\n".formatted(index, index));
        }
        return fields.toString();
    }

    private static void writeSynthetic(Path corpus, String workload, String typeName, int count) throws Exception {
        Path file = corpus.resolve(workload).resolve(typeName + ".java");
        Files.createDirectories(file.getParent());
        Files.writeString(
                file,
                readTemplate("flat-template.java.txt")
                        .replace("${name}", typeName)
                        .replace("${members}", renderFields(count, "    ")));
    }

    /** Holds a worker's model and the immutable serialization contract captured before measurement. */
    @Value
    @AllArgsConstructor(access = PRIVATE)
    static class PreparedInput {
        @NonNull
        String id;

        @NonNull
        SpoonAstModel model;

        @NonNull
        String expectedSrcCode;

        @NonNull
        String expectedRanges;

        @NonNull
        String modelFingerprint;
    }
}
