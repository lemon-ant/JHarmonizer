// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.printerlab;

import com.sun.management.OperatingSystemMXBean;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.MemPoolProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/** Command-line entry point for the temporary, independent printer experiment. */
@UtilityClass
public class PrinterLab {
    private static final String PROTOCOL = "printer-lab-v1";

    /**
     * Runs capture, smoke, metrics, report, compare or archive; see README.md for arguments.
     * @param arguments command, repository/run paths and optional comparison baseline
     */
    public static void main(@NonNull String[] arguments) throws Exception {
        if (Runtime.version().feature() != 21) {
            throw new IllegalStateException("Use JDK 21 for this protocol");
        }
        String injectedOptions = System.getenv().getOrDefault("JAVA_TOOL_OPTIONS", "") + " "
                + System.getenv().getOrDefault("JDK_JAVA_OPTIONS", "") + " "
                + ManagementFactory.getRuntimeMXBean().getInputArguments();
        if (injectedOptions.contains("-javaagent")
                || injectedOptions.contains("-agentlib")
                || injectedOptions.contains("-agentpath")) {
            throw new IllegalStateException("Run measurements without debugger, coverage, or instrumentation agents");
        }
        if (arguments.length < 3) {
            throw new IllegalArgumentException(
                    "Usage: capture|smoke|metrics REPOSITORY OUTPUT [BASELINE]; report RUN OUTPUT; compare BASELINE"
                            + " CANDIDATE OUTPUT");
        }
        String command = arguments[0];
        if (command.equals("report")) {
            MeasurementReport.write(Path.of(arguments[1]), Path.of(arguments[2]));
            return;
        }
        if (command.equals("archive")) {
            MeasurementReport.archive(Path.of(arguments[1]), Path.of(arguments[2]));
            return;
        }
        if (command.equals("compare")) {
            if (arguments.length != 4) {
                throw new IllegalArgumentException("compare BASELINE CANDIDATE OUTPUT");
            }
            MeasurementReport.compare(Path.of(arguments[1]), Path.of(arguments[2]), Path.of(arguments[3]));
            return;
        }
        if (!List.of("capture", "smoke", "metrics").contains(command)) {
            throw new IllegalArgumentException("Unknown command: " + command);
        }
        Path repository = Path.of(arguments[1]).toAbsolutePath().normalize();
        Path output = Path.of(arguments[2]).toAbsolutePath().normalize();
        if (Files.exists(output)) {
            throw new IllegalArgumentException("Preserve existing measurements; choose a new run directory: " + output);
        }
        verifyBuild(repository);
        Files.createDirectories(output);
        boolean smoke = command.equals("smoke");
        Map<String, Object> metadata = collectMetadata(repository, smoke);
        LabFiles.writeJson(output.resolve("environment.json"), metadata);
        System.out.println("Collecting source and architecture metrics: " + output);
        List<SourceMetrics.ScopeEntry> scope = SourceMetrics.measure(repository, output);
        ArchitectureMetrics.measure(repository, output, scope);
        if (command.equals("metrics")) {
            System.out.println("Static measurements complete: " + output);
            return;
        }
        System.out.println("Preparing and compiling frozen printer workloads");
        List<Map<String, Object>> workloads = PrinterWorkloads.create(repository, output);
        LabFiles.writeJson(output.resolve("workloads.json"), workloads);
        if (arguments.length == 4) {
            MeasurementReport.requireComparable(Path.of(arguments[3]), output);
        }
        for (int threads : smoke ? List.of(8) : List.of(1, 2, 4, 8)) {
            runBenchmarks(output, Mode.Throughput, threads, smoke);
        }
        runBenchmarks(output, Mode.SampleTime, 1, smoke);
        metadata = new LinkedHashMap<>(metadata);
        metadata.put("completedAt", Instant.now().toString());
        LabFiles.writeJson(output.resolve("environment.json"), Map.copyOf(metadata));
        if (!smoke) {
            MeasurementReport.requireComplete(output);
        }
        MeasurementReport.write(output, output.resolve("measurements.md"));
        System.out.println("Measurements complete: " + output);
    }

    private static void runBenchmarks(Path output, Mode mode, int threads, boolean smoke) throws Exception {
        String name = (mode == Mode.Throughput ? "throughput" : "sample") + "-t" + threads;
        Path raw = output.resolve(name + ".raw.json");
        Path log = output.resolve(name + ".log");
        System.out.println("JMH " + name + ": " + (smoke ? "diagnostic run" : "3 forks, 4 workloads"));
        OptionsBuilder options = new OptionsBuilder();
        options.include(PrinterBenchmark.class.getName() + ".serialize")
                .mode(mode)
                .timeUnit(
                        mode == Mode.Throughput
                                ? java.util.concurrent.TimeUnit.SECONDS
                                : java.util.concurrent.TimeUnit.MICROSECONDS)
                .threads(threads)
                .forks(smoke ? 1 : 3)
                .warmupIterations(smoke ? 1 : 5)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(smoke ? 1 : 5)
                .measurementTime(TimeValue.seconds(smoke ? 1 : 2))
                .shouldFailOnError(true)
                .shouldDoGC(false)
                .addProfiler(GCProfiler.class)
                .addProfiler(MemPoolProfiler.class)
                .jvmArgs(
                        "-Xms1g",
                        "-Xmx1g",
                        "-XX:+UseG1GC",
                        "-Dfile.encoding=UTF-8",
                        "-Duser.language=en",
                        "-Duser.country=US",
                        "-Duser.timezone=UTC",
                        "-Dprinter.lab.corpus=" + output.resolve("corpus"))
                .resultFormat(ResultFormatType.JSON)
                .result(raw.toString())
                .output(log.toString());
        new Runner(options.build()).run();
        LabFiles.writeJson(output.resolve(name + ".json"), LabFiles.readRawJson(raw));
        LabFiles.licenseLog(log);
    }

    private static void verifyBuild(Path repository) throws Exception {
        String dirty = LabFiles.execute(
                repository,
                List.of(
                        "git",
                        "status",
                        "--porcelain",
                        "--",
                        "core/src/main",
                        "core/pom.xml",
                        "dependency-aware-sorting/src/main",
                        "dependency-aware-sorting/pom.xml",
                        "pom.xml"));
        if (!dirty.isBlank()) {
            throw new IllegalStateException(
                    "Commit production changes before measuring so the revision identifies the implementation:\n"
                            + dirty);
        }
        Path loaded = Path.of(SpoonParser.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
        Path built = repository.resolve("core/target").resolve(loaded.getFileName());
        if (!Files.isRegularFile(loaded) || !Files.isRegularFile(built) || Files.mismatch(loaded, built) != -1) {
            throw new IllegalStateException("Lab/core artifact mismatch. Install core, then rebuild the lab. Loaded="
                    + loaded + ", built=" + built);
        }
    }

    @NonNull
    private static Map<String, Object> collectMetadata(Path repository, boolean smoke) throws Exception {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("protocol", PROTOCOL);
        metadata.put("diagnosticOnly", smoke);
        metadata.put("startedAt", Instant.now().toString());
        metadata.put("revision", LabFiles.execute(repository, List.of("git", "rev-parse", "HEAD")));
        metadata.put("javaVersion", Runtime.version().toString());
        metadata.put("javaVendor", System.getProperty("java.vendor"));
        metadata.put("javaVm", System.getProperty("java.vm.name"));
        metadata.put("osName", System.getProperty("os.name"));
        metadata.put("osVersion", System.getProperty("os.version"));
        metadata.put("osArch", System.getProperty("os.arch"));
        metadata.put("logicalProcessors", Runtime.getRuntime().availableProcessors());
        OperatingSystemMXBean operatingSystem = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        metadata.put("physicalMemoryBytes", operatingSystem.getTotalMemorySize());
        metadata.put("freePhysicalMemoryBytesAtStart", operatingSystem.getFreeMemorySize());
        metadata.put(
                "processorIdentifier",
                System.getenv().getOrDefault("PROCESSOR_IDENTIFIER", "not exposed by OS environment"));
        metadata.put(
                "machineIdHash",
                LabFiles.hash(System.getenv()
                        .getOrDefault("COMPUTERNAME", System.getenv().getOrDefault("HOSTNAME", "unknown"))));
        metadata.put("javaToolOptions", System.getenv().getOrDefault("JAVA_TOOL_OPTIONS", ""));
        metadata.put("jdkJavaOptions", System.getenv().getOrDefault("JDK_JAVA_OPTIONS", ""));
        metadata.put("hostJvmArguments", ManagementFactory.getRuntimeMXBean().getInputArguments());
        metadata.put(
                "printerConfig",
                Map.of(
                        "blankLineAfterTypeHeader",
                        true,
                        "blankLineBeforeComment",
                        true,
                        "blankLineBetweenFields",
                        true));
        metadata.put(
                "forkJvmArguments",
                List.of(
                        "-Xms1g",
                        "-Xmx1g",
                        "-XX:+UseG1GC",
                        "-Dfile.encoding=UTF-8",
                        "-Duser.language=en",
                        "-Duser.country=US",
                        "-Duser.timezone=UTC"));
        Map<String, String> protocolFiles = new LinkedHashMap<>();
        Path lab = repository.resolve("core/printer-lab");
        List<String> protocolPaths = new ArrayList<>(List.of("pom.xml", "corpus.tsv"));
        try (Stream<Path> paths = Files.walk(lab.resolve("src/main"))) {
            paths.filter(Files::isRegularFile)
                    .sorted()
                    .forEach(path ->
                            protocolPaths.add(lab.relativize(path).toString().replace('\\', '/')));
        }
        for (String file : protocolPaths) {
            protocolFiles.put(
                    file, LabFiles.hash(Files.readString(lab.resolve(file)).replace("\r\n", "\n")));
        }
        metadata.put("workloadAndHarnessHashes", Map.copyOf(protocolFiles));
        metadata.put(
                "scopeManifestSha256",
                LabFiles.hash(Files.readString(lab.resolve("scope.tsv")).replace("\r\n", "\n")));
        Map<String, String> libraries = new java.util.TreeMap<>();
        try (Stream<Path> paths = Files.list(lab.resolve("target/lib"))) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".jar"))
                    .toList()) {
                libraries.put(path.getFileName().toString(), LabFiles.hash(Files.readAllBytes(path)));
            }
        }
        metadata.put("runtimeLibraries", Map.copyOf(libraries));
        return Map.copyOf(metadata);
    }
}
