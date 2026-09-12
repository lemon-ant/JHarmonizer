// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.printerlab;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Summarizes original PMD/ArchUnit/JMH values and compares compatible experiment runs. */
@UtilityClass
final class MeasurementReport {

    /**
     * Summarizes source, architecture and JMH evidence.
     * @param run captured results
     * @param destination Markdown report destination
     */
    static void write(@NonNull Path run, @NonNull Path destination) throws Exception {
        JsonNode environment = LabFiles.readJson(run.resolve("environment.json"));
        JsonNode source = LabFiles.readJson(run.resolve("source-metrics.json"));
        JsonNode architecture = LabFiles.readJson(run.resolve("architecture.json"));
        StringBuilder report = new StringBuilder("# Printer measurements\n\n");
        report.append("Revision: `")
                .append(environment.path("revision").asText())
                .append("`. Protocol: `")
                .append(environment.path("protocol").asText())
                .append("`. Diagnostic only: **")
                .append(environment.path("diagnosticOnly").asBoolean())
                .append("**.\n\n");
        report.append("JDK ")
                .append(environment.path("javaVersion").asText())
                .append("; ")
                .append(environment.path("logicalProcessors").asInt())
                .append(" logical processors; ")
                .append(environment.path("osName").asText())
                .append(".\n\n");
        report.append("## Source size and structure\n\n| Metric | Value |\n| --- | ---: |\n");
        for (String key : List.of(
                "sourceFiles",
                "declaredTypesInWholeFiles",
                "declaredOperations",
                "physicalLinesSelected",
                "ncssSelected",
                "sumCyclo",
                "sumCognitive",
                "shortCircuitOperators",
                "cycloOver10",
                "cognitiveOver15")) {
            report.append("| ")
                    .append(key)
                    .append(" | ")
                    .append(source.path("summary").path(key))
                    .append(" |\n");
        }
        report.append("\n| Per-operation metric | Mean | p50 | p95 | Max |\n| --- | ---: | ---: | ---: | ---: |\n");
        for (String key : List.of("loc", "ncss", "cyclo", "cognitive", "npath", "fanOut", "arity")) {
            JsonNode values = source.path("summary").path(key);
            report.append("| ").append(key);
            for (String statistic : List.of("mean", "p50", "p95", "max")) {
                report.append(" | ").append(number(values.path(statistic).asDouble()));
            }
            report.append(" |\n");
        }
        report.append("\n| Source construct | Occurrences |\n| --- | ---: |\n");
        for (String key : List.of(
                "IfStatement",
                "ConditionalExpression",
                "ForStatement",
                "ForeachStatement",
                "WhileStatement",
                "DoStatement",
                "SwitchStatement",
                "SwitchExpression",
                "CatchClause",
                "LambdaExpression")) {
            report.append("| ")
                    .append(key)
                    .append(" | ")
                    .append(source.path("summary")
                            .path("astNodeInventory")
                            .path(key)
                            .asInt())
                    .append(" |\n");
        }
        report.append("\n"
                + "## Complexity hotspots\n\n"
                + "| Method | Lines | NCSS | Cyclo | Cognitive | NPath |\n"
                + "| --- | ---: | ---: | ---: | ---: | ---: |\n");
        List<JsonNode> methods = new ArrayList<>();
        source.path("methods").forEach(methods::add);
        methods.sort(Comparator.comparingInt(
                        (JsonNode method) -> method.path("cognitive").asInt())
                .reversed()
                .thenComparing(method -> method.path("id").asText()));
        for (JsonNode method : methods.stream().limit(12).toList()) {
            report.append("| `")
                    .append(method.path("type").asText().replace("io.github.lemon_ant.jharmonizer.core.", ""))
                    .append('#')
                    .append(method.path("name").asText())
                    .append("` |");
            for (String metric : List.of("loc", "ncss", "cyclo", "cognitive", "npath")) {
                report.append(' ').append(method.path(metric)).append(" |");
            }
            report.append('\n');
        }
        report.append("\n## Architecture\n\n| Metric | Value |\n| --- | ---: |\n");
        for (String key : List.of("internalEdges", "cycleCount", "CCD", "ACD", "RACD", "NCCD")) {
            report.append("| ")
                    .append(key)
                    .append(" | ")
                    .append(architecture.path(key))
                    .append(" |\n");
        }
        report.append("\nSee `architecture.md` for the graph and `architecture.json` for every dependency.\n\n");
        report.append("## Throughput and allocation\n\n"
                        + "An operation serializes the entire workload batch. JMH error is its reported confidence"
                        + " half-width.\n\n")
                .append("| Workload | Threads | Batches/s | JMH error | Documents/s | Allocated B/batch | Sum of pool"
                        + " peaks MiB |\n")
                .append("| --- | ---: | ---: | ---: | ---: | ---: | ---: |\n");
        Map<String, Integer> batchSizes = new LinkedHashMap<>();
        LabFiles.readJson(run.resolve("workloads.json"))
                .forEach(workload -> batchSizes.put(
                        workload.path("name").asText(),
                        workload.path("documentsPerOperation").asInt()));
        for (JsonNode benchmark : readBenchmarks(run)) {
            if (!benchmark.path("mode").asText().equals("thrpt")) {
                continue;
            }
            String workload = benchmark.path("params").path("workload").asText();
            double score = benchmark.path("primaryMetric").path("score").asDouble();
            report.append("| ")
                    .append(workload)
                    .append(" | ")
                    .append(benchmark.path("threads").asInt())
                    .append(" | ")
                    .append(number(score))
                    .append(" | ")
                    .append(number(
                            benchmark.path("primaryMetric").path("scoreError").asDouble()))
                    .append(" | ")
                    .append(number(score * batchSizes.get(workload)))
                    .append(" | ")
                    .append(number(secondary(benchmark, "gc.alloc.rate.norm")))
                    .append(" | ")
                    .append(number(secondary(benchmark, "mempool.total.used") / 1024.0))
                    .append(" |\n");
        }
        report.append("\n"
                + "Pool peaks include prepared ASTs and JVM overhead. The sum need not occur simultaneously; it is"
                + " not RSS, retained printer size, or bytes allocated per call.\n\n");
        Map<String, JsonNode> indexed = indexBenchmarks(run);
        report.append("## Throughput scaling\n\n"
                        + "Ratios of independently measured means; no confidence interval is inferred for these"
                        + " ratios.\n\n")
                .append("| Workload | Threads | Speedup over one worker | Efficiency % |\n"
                        + "| --- | ---: | ---: | ---: |\n");
        for (String workload : PrinterWorkloads.NAMES) {
            JsonNode single = indexed.get("thrpt/" + workload + "/t1");
            if (single == null) {
                continue;
            }
            for (int threads : List.of(1, 2, 4, 8)) {
                JsonNode benchmark = indexed.get("thrpt/" + workload + "/t" + threads);
                double speedup = benchmark.path("primaryMetric").path("score").asDouble()
                        / single.path("primaryMetric").path("score").asDouble();
                report.append("| ")
                        .append(workload)
                        .append(" | ")
                        .append(threads)
                        .append(" | ")
                        .append(number(speedup))
                        .append(" | ")
                        .append(number(100 * speedup / threads))
                        .append(" |\n");
            }
        }
        report.append('\n')
                .append("## Sampled batch service time\n\n"
                        + "One worker; no arrival queue. These quantiles describe whole batches, not individual"
                        + " files.\n\n")
                .append("| Workload | p50 µs | p95 µs | p99 µs |\n| --- | ---: | ---: | ---: |\n");
        for (JsonNode benchmark : readBenchmarks(run)) {
            if (!benchmark.path("mode").asText().equals("sample")) {
                continue;
            }
            report.append("| ").append(benchmark.path("params").path("workload").asText());
            for (String percentile : List.of("50.0", "95.0", "99.0")) {
                report.append(" | ")
                        .append(number(benchmark
                                .path("primaryMetric")
                                .path("scorePercentiles")
                                .path(percentile)
                                .asDouble()));
            }
            report.append(" |\n");
        }
        LabFiles.writeMarkdown(destination, report.toString());
    }

    /**
     * Rejects incompatible protocols, environments and serialization contracts before timing.
     * @param baseline reference run
     * @param candidate run to validate before comparing timings
     */
    static void requireComparable(@NonNull Path baseline, @NonNull Path candidate) throws Exception {
        requireComplete(baseline);
        JsonNode previous = LabFiles.readJson(baseline.resolve("environment.json"));
        JsonNode current = LabFiles.readJson(candidate.resolve("environment.json"));
        if (previous.path("diagnosticOnly").asBoolean()
                || current.path("diagnosticOnly").asBoolean()) {
            throw new IllegalArgumentException("Smoke measurements cannot serve as comparison results");
        }
        for (String key : List.of(
                "protocol",
                "javaVersion",
                "javaVendor",
                "javaVm",
                "osName",
                "osVersion",
                "osArch",
                "logicalProcessors",
                "physicalMemoryBytes",
                "processorIdentifier",
                "machineIdHash",
                "javaToolOptions",
                "jdkJavaOptions",
                "printerConfig",
                "forkJvmArguments",
                "workloadAndHarnessHashes")) {
            if (!previous.path(key).equals(current.path(key))) {
                throw new IllegalStateException("Comparison protocol/environment differs: " + key);
            }
        }
        if (!LabFiles.readJson(baseline.resolve("golden.json"))
                .equals(LabFiles.readJson(candidate.resolve("golden.json")))) {
            throw new IllegalStateException("Input, prepared model, output or skipped ranges differ from the baseline");
        }
        JsonNode previousLibraries = previous.path("runtimeLibraries");
        JsonNode currentLibraries = current.path("runtimeLibraries");
        Set<String> libraries = new TreeSet<>(iterableFields(previousLibraries));
        libraries.addAll(iterableFields(currentLibraries));
        for (String library : libraries) {
            if (!library.startsWith("jharmonizer-core-")
                    && !library.startsWith("dependency-aware-sorting-")
                    && !previousLibraries.path(library).equals(currentLibraries.path(library))) {
                throw new IllegalStateException("External runtime dependency changed: " + library);
            }
        }
    }

    /**
     * Compares only complete compatible captures.
     * @param baseline reference run
     * @param candidate replacement run
     * @param destination Markdown comparison
     */
    static void compare(@NonNull Path baseline, @NonNull Path candidate, @NonNull Path destination) throws Exception {
        requireComparable(baseline, candidate);
        requireComplete(candidate);
        Map<String, JsonNode> previous = indexBenchmarks(baseline);
        Map<String, JsonNode> current = indexBenchmarks(candidate);
        if (!previous.keySet().equals(current.keySet())) {
            throw new IllegalStateException("Benchmark combinations differ; one run may be incomplete");
        }
        StringBuilder report = new StringBuilder("# Printer comparison\n\n"
                        + "Protocol, hardware identity, runtime, corpus and serialized output checks passed.\n\n")
                .append("| Benchmark | Baseline | Candidate | Change % | Allocated B/batch change % | JMH mean CIs"
                        + " overlap |\n")
                .append("| --- | ---: | ---: | ---: | ---: | --- |\n");
        for (String key : previous.keySet().stream().sorted().toList()) {
            JsonNode before = previous.get(key);
            JsonNode after = current.get(key);
            double oldScore = before.path("primaryMetric").path("score").asDouble();
            double newScore = after.path("primaryMetric").path("score").asDouble();
            JsonNode oldInterval = before.path("primaryMetric").path("scoreConfidence");
            JsonNode newInterval = after.path("primaryMetric").path("scoreConfidence");
            boolean overlaps = oldInterval.get(0).asDouble()
                            <= newInterval.get(1).asDouble()
                    && newInterval.get(0).asDouble() <= oldInterval.get(1).asDouble();
            report.append("| ")
                    .append(key)
                    .append(" | ")
                    .append(number(oldScore))
                    .append(" | ")
                    .append(number(newScore))
                    .append(" | ")
                    .append(number(100 * (newScore / oldScore - 1)))
                    .append(" | ")
                    .append(number(100
                            * (secondary(after, "gc.alloc.rate.norm") / secondary(before, "gc.alloc.rate.norm") - 1)))
                    .append(" | ")
                    .append(overlaps)
                    .append(" |\n");
        }
        report.append("\n"
                        + "Higher throughput is better; lower sampled time and allocation are better. CI overlap"
                        + " is descriptive and is not a significance test.\n\n")
                .append("## Source metric changes\n\n| Metric | Baseline | Candidate |\n| --- | ---: | ---: |\n");
        JsonNode oldSource =
                LabFiles.readJson(baseline.resolve("source-metrics.json")).path("summary");
        JsonNode newSource =
                LabFiles.readJson(candidate.resolve("source-metrics.json")).path("summary");
        for (String key : iterableFields(oldSource)) {
            report.append("| ")
                    .append(key)
                    .append(" | ")
                    .append(oldSource.get(key))
                    .append(" | ")
                    .append(newSource.get(key))
                    .append(" |\n");
        }
        JsonNode oldArchitecture = LabFiles.readJson(baseline.resolve("architecture.json"));
        JsonNode newArchitecture = LabFiles.readJson(candidate.resolve("architecture.json"));
        report.append("\n## Architecture changes\n\n| Metric | Baseline | Candidate |\n| --- | ---: | ---: |\n");
        for (String key : List.of("internalEdges", "cycleCount", "CCD", "ACD", "RACD", "NCCD")) {
            report.append("| ")
                    .append(key)
                    .append(" | ")
                    .append(oldArchitecture.path(key))
                    .append(" | ")
                    .append(newArchitecture.path(key))
                    .append(" |\n");
        }
        for (String key : List.of("classes", "distinctExternalLibraryTargets")) {
            report.append("| ")
                    .append(key)
                    .append(" count | ")
                    .append(oldArchitecture.path(key).size())
                    .append(" | ")
                    .append(newArchitecture.path(key).size())
                    .append(" |\n");
        }
        report.append("| CPD clone groups | ")
                .append(LabFiles.readJson(baseline.resolve("cpd-summary.json")).path("cloneGroups"))
                .append(" | ")
                .append(LabFiles.readJson(candidate.resolve("cpd-summary.json")).path("cloneGroups"))
                .append(" |\n");
        report.append("| PMD findings | ")
                .append(LabFiles.readJson(baseline.resolve("maintainability-findings.json"))
                        .path("findings")
                        .size())
                .append(" | ")
                .append(LabFiles.readJson(candidate.resolve("maintainability-findings.json"))
                        .path("findings")
                        .size())
                .append(" |\n");
        LabFiles.writeMarkdown(destination, report.toString());
    }

    /** Validates a complete, non-diagnostic matrix before archiving or comparing it.
     * @param run completed capture directory
     */
    static void requireComplete(@NonNull Path run) throws Exception {
        JsonNode environment = LabFiles.readJson(run.resolve("environment.json"));
        if (environment.path("diagnosticOnly").asBoolean() || !environment.hasNonNull("completedAt")) {
            throw new IllegalStateException("A completed full capture is required: " + run);
        }
        Map<String, JsonNode> results = indexBenchmarks(run);
        Set<String> expected = new TreeSet<>();
        for (String workload : PrinterWorkloads.NAMES) {
            for (int threads : List.of(1, 2, 4, 8)) {
                expected.add("thrpt/" + workload + "/t" + threads);
            }
            expected.add("sample/" + workload + "/t1");
        }
        if (!results.keySet().equals(expected)) {
            throw new IllegalStateException("Full JMH matrix is missing or has extra results: " + run);
        }
        for (JsonNode benchmark : results.values()) {
            double score = benchmark.path("primaryMetric").path("score").asDouble(Double.NaN);
            if (!benchmark.path("jmhVersion").asText().equals("1.37")
                    || benchmark.path("forks").asInt() != 3
                    || benchmark.path("warmupIterations").asInt() != 5
                    || !benchmark.path("warmupTime").asText().equals("1 s")
                    || benchmark.path("measurementIterations").asInt() != 5
                    || !benchmark.path("measurementTime").asText().equals("2 s")
                    || !Double.isFinite(score)
                    || score <= 0) {
                throw new IllegalStateException("JMH result does not follow the frozen protocol: " + run);
            }
            secondary(benchmark, "gc.alloc.rate.norm");
            secondary(benchmark, "mempool.total.used");
        }
    }

    /** Copies the complete licensed evidence while leaving generated scratch files under target.
     * @param run completed capture directory
     * @param destination new archive directory
     */
    static void archive(@NonNull Path run, @NonNull Path destination) throws Exception {
        requireComplete(run);
        List<String> files = new ArrayList<>(List.of(
                "environment.json",
                "workloads.json",
                "golden.json",
                "source-metrics.json",
                "maintainability-findings.json",
                "architecture.json",
                "architecture.md",
                "cpd.xml",
                "cpd-summary.json",
                "measurements.md"));
        for (String name : List.of("throughput-t1", "throughput-t2", "throughput-t4", "throughput-t8", "sample-t1")) {
            files.add(name + ".json");
            files.add(name + ".log");
        }
        for (String file : files) {
            if (!Files.isRegularFile(run.resolve(file))) {
                throw new IllegalStateException("Missing capture artifact: " + file);
            }
        }
        if (Files.exists(destination)) {
            throw new IllegalArgumentException("Archive directory already exists: " + destination);
        }
        Files.createDirectories(destination);
        for (String file : files) {
            Files.copy(run.resolve(file), destination.resolve(file));
        }
    }

    @NonNull
    private static Map<String, JsonNode> indexBenchmarks(Path run) throws Exception {
        Map<String, JsonNode> indexed = new TreeMap<>();
        for (JsonNode benchmark : readBenchmarks(run)) {
            String key = benchmark.path("mode").asText() + "/"
                    + benchmark.path("params").path("workload").asText() + "/t"
                    + benchmark.path("threads").asInt();
            if (indexed.put(key, benchmark) != null) {
                throw new IllegalStateException("Duplicate benchmark result: " + key);
            }
        }
        return Map.copyOf(indexed);
    }

    @NonNull
    private static List<JsonNode> readBenchmarks(Path run) throws Exception {
        List<JsonNode> results = new ArrayList<>();
        try (Stream<Path> files = Files.list(run)) {
            for (Path path : files.filter(
                            file -> file.getFileName().toString().matches("(throughput|sample)-t[0-9]+\\.json"))
                    .sorted()
                    .toList()) {
                LabFiles.readJson(path).forEach(results::add);
            }
        }
        return results.stream()
                .sorted(Comparator.comparing(
                        benchmark -> benchmark.path("params").path("workload").asText() + "/"
                                + benchmark.path("threads").asInt() + "/"
                                + benchmark.path("mode").asText()))
                .toList();
    }

    private static double secondary(JsonNode benchmark, String name) {
        JsonNode value = benchmark.path("secondaryMetrics").path(name).path("score");
        if (!value.isNumber()) {
            throw new IllegalStateException("Required JMH profiler result missing: " + name);
        }
        return value.asDouble();
    }

    @NonNull
    private static List<String> iterableFields(JsonNode node) {
        List<String> fields = new ArrayList<>();
        node.fieldNames().forEachRemaining(fields::add);
        return List.copyOf(fields);
    }

    @NonNull
    private static String number(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
