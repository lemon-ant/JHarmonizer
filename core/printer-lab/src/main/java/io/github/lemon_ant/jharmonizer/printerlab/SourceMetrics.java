// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.printerlab;

import static lombok.AccessLevel.PRIVATE;
import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.COGNITIVE_COMPLEXITY;
import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.CYCLO;
import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.FAN_OUT;
import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.LINES_OF_CODE;
import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.NCSS;
import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.NPATH_COMP;
import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.TIGHT_CLASS_COHESION;
import static net.sourceforge.pmd.lang.java.metrics.JavaMetrics.WEIGHED_METHOD_COUNT;
import static net.sourceforge.pmd.lang.metrics.MetricsUtil.computeMetric;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.cpd.CPDConfiguration;
import net.sourceforge.pmd.cpd.CpdAnalysis;
import net.sourceforge.pmd.cpd.XMLRenderer;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.document.FileLocation;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.ast.ASTExecutableDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTInfixExpression;
import net.sourceforge.pmd.lang.java.ast.ASTMethodCall;
import net.sourceforge.pmd.lang.java.ast.ASTTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.BinaryOp;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.jspecify.annotations.Nullable;

/** Exports PMD's metrics without replacing their algorithms with custom counters. */
final class SourceMetrics {
    @NonNull
    private final Map<Path, ScopeEntry> scope;

    @NonNull
    private final List<Map<String, Object>> files = new ArrayList<>();

    @NonNull
    private final List<Map<String, Object>> types = new ArrayList<>();

    @NonNull
    private final List<Map<String, Object>> methods = new ArrayList<>();

    private SourceMetrics(Map<Path, ScopeEntry> scope) {
        this.scope = scope;
    }

    /**
     * Exports selected source metrics, findings and duplication evidence.
     * @param repository checkout root
     * @param output run directory
     * @return the measured source scope
     */
    @NonNull
    static List<ScopeEntry> measure(@NonNull Path repository, @NonNull Path output) throws Exception {
        Map<Path, ScopeEntry> entries = new LinkedHashMap<>();
        for (String line : LabFiles.readManifest(repository.resolve("core/printer-lab/scope.tsv"))) {
            String[] columns = line.split("\t");
            Path path =
                    repository.resolve("core/src/main/java").resolve(columns[1]).normalize();
            if (!Files.isRegularFile(path) || columns.length != 3) {
                throw new IllegalArgumentException("Invalid scope entry: " + line);
            }
            entries.put(path, new ScopeEntry(columns[0], columns[1], Set.of(columns[2].split(","))));
        }
        SourceMetrics metrics = new SourceMetrics(Map.copyOf(entries));
        PMDConfiguration configuration = new PMDConfiguration();
        configuration.setThreads(0);
        configuration.setIgnoreIncrementalAnalysis(true);
        configuration.setSourceEncoding(StandardCharsets.UTF_8);
        configuration.setDefaultLanguageVersion(
                LanguageRegistry.PMD.getLanguageById("java").getVersion("21"));
        configuration.setClassLoader(SourceMetrics.class.getClassLoader());
        try (PmdAnalysis analysis = PmdAnalysis.create(configuration)) {
            analysis.addRuleSet(RuleSet.forSingleRule(metrics.new ExportRule()));
            analysis.addRuleSet(analysis.newRuleSetLoader().loadFromResource("maintainability-rules.xml"));
            entries.keySet().forEach(path -> analysis.files().addFile(path));
            Report report = analysis.performAnalysisAndCollectReport();
            if (!report.getProcessingErrors().isEmpty()
                    || !report.getConfigurationErrors().isEmpty()) {
                throw new IllegalStateException(
                        "PMD analysis errors: " + report.getProcessingErrors() + report.getConfigurationErrors());
            }
            List<Map<String, Object>> findings = new ArrayList<>();
            for (RuleViolation violation : report.getViolations()) {
                ScopeEntry entry = entries.get(
                        Path.of(violation.getFileId().getAbsolutePath()).normalize());
                boolean selected = entry.isWholeType()
                        || metrics.methods.stream()
                                .anyMatch(method -> entry.getPath().equals(method.get("path"))
                                        && violation.getBeginLine() >= ((Number) method.get("beginLine")).intValue()
                                        && violation.getEndLine() <= ((Number) method.get("endLine")).intValue());
                if (selected) {
                    findings.add(Map.of(
                            "path",
                            entry.getPath(),
                            "line",
                            violation.getBeginLine(),
                            "rule",
                            violation.getRule().getName(),
                            "description",
                            violation.getDescription()));
                }
            }
            findings.sort(Comparator.comparing(
                    finding -> finding.get("path") + ":" + finding.get("line") + ":" + finding.get("rule")));
            LabFiles.writeJson(
                    output.resolve("maintainability-findings.json"),
                    Map.of(
                            "findings",
                            List.copyOf(findings),
                            "suppressedInAnalyzedFiles",
                            report.getSuppressedViolations().size()));
        }
        if (metrics.files.size() != entries.size() || metrics.methods.isEmpty()) {
            throw new IllegalStateException("PMD did not visit the complete scope");
        }
        metrics.files.sort(Comparator.comparing(row -> row.get("path").toString()));
        metrics.types.sort(Comparator.comparing(row -> row.get("type").toString()));
        metrics.methods.sort(Comparator.comparing(row -> row.get("id").toString()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tool", "PMD 7.24.0; default metric options; source before Lombok expansion");
        result.put("files", List.copyOf(metrics.files));
        result.put("types", List.copyOf(metrics.types));
        result.put("methods", List.copyOf(metrics.methods));
        result.put("summary", metrics.summarize());
        LabFiles.writeJson(output.resolve("source-metrics.json"), Map.copyOf(result));
        metrics.detectDuplication(output);
        return List.copyOf(entries.values());
    }

    private void collect(ASTCompilationUnit unit) {
        Path path =
                Path.of(unit.getTextDocument().getFileId().getAbsolutePath()).normalize();
        ScopeEntry entry = scope.get(path);
        if (entry == null) {
            throw new IllegalStateException("Unexpected PMD input: " + path);
        }
        try {
            String source = Files.readString(path).replace("\r\n", "\n");
            List<String> lines = source.lines().toList();
            List<Map<String, Object>> selectedMethods = new ArrayList<>();
            for (ASTTypeDeclaration type :
                    unit.descendants(ASTTypeDeclaration.class).crossFindBoundaries()) {
                if (entry.isWholeType()) {
                    Map<String, Object> row = new LinkedHashMap<>(common(entry, type));
                    row.put("type", type.getBinaryName());
                    row.put("topLevel", type.isTopLevel());
                    row.put("loc", computeMetric(LINES_OF_CODE, type));
                    row.put("ncss", computeMetric(NCSS, type));
                    row.put("fanOut", computeMetric(FAN_OUT, type));
                    if (WEIGHED_METHOD_COUNT.supports(type)) {
                        row.put("wmc", computeMetric(WEIGHED_METHOD_COUNT, type));
                        row.put("tcc", computeMetric(TIGHT_CLASS_COHESION, type));
                    }
                    types.add(Map.copyOf(row));
                }
                for (ASTExecutableDeclaration method : type.getOperations()) {
                    if (!entry.isWholeType() && !entry.getMethods().contains(method.getName())) {
                        continue;
                    }
                    Map<String, Object> row = new LinkedHashMap<>(common(entry, method));
                    row.put("type", type.getBinaryName());
                    row.put("name", method.getName());
                    row.put(
                            "id",
                            type.getBinaryName() + "#" + method.getName() + "/" + method.getArity() + "@"
                                    + method.getBeginLine());
                    row.put("arity", method.getArity());
                    row.put("loc", computeMetric(LINES_OF_CODE, method));
                    row.put("ncss", computeMetric(NCSS, method));
                    row.put("cyclo", computeMetric(CYCLO, method));
                    row.put("cognitive", computeMetric(COGNITIVE_COMPLEXITY, method));
                    row.put("npath", computeMetric(NPATH_COMP, method));
                    row.put("fanOut", computeMetric(FAN_OUT, method));
                    Map<String, Object> immutable = Map.copyOf(row);
                    methods.add(immutable);
                    selectedMethods.add(immutable);
                }
            }
            if (!entry.isWholeType()
                    && selectedMethods.size() != entry.getMethods().size()) {
                throw new IllegalStateException("Partial scope must name exactly one declaration per method: " + entry);
            }
            Map<String, Object> file = new LinkedHashMap<>();
            file.put("path", entry.getPath());
            file.put("role", entry.getRole());
            file.put("wholeType", entry.isWholeType());
            file.put("sourceSha256", LabFiles.hash(source));
            file.put("physicalFileLines", lines.size());
            file.put("blankFileLines", lines.stream().filter(String::isBlank).count());
            file.put(
                    "selectedPhysicalLines",
                    entry.isWholeType()
                            ? lines.size()
                            : selectedMethods.stream()
                                    .mapToInt(row -> ((Number) row.get("loc")).intValue())
                                    .sum());
            file.put(
                    "selectedNcss",
                    entry.isWholeType()
                            ? computeMetric(NCSS, unit)
                            : selectedMethods.stream()
                                    .mapToInt(row -> ((Number) row.get("ncss")).intValue())
                                    .sum());
            Map<String, Long> inventory = new TreeMap<>();
            Map<String, Long> calls = new TreeMap<>();
            int shortCircuitOperators = 0;
            for (Node node : unit.descendants().crossFindBoundaries()) {
                if (entry.isWholeType() || selectedMethods.stream().anyMatch(row -> within(node, row))) {
                    inventory.merge(node.getXPathNodeName(), 1L, Long::sum);
                    if (node instanceof ASTMethodCall call) {
                        calls.merge(call.getMethodName(), 1L, Long::sum);
                    }
                    if (node instanceof ASTInfixExpression expression
                            && (expression.getOperator() == BinaryOp.CONDITIONAL_AND
                                    || expression.getOperator() == BinaryOp.CONDITIONAL_OR)) {
                        shortCircuitOperators++;
                    }
                }
            }
            file.put("astNodeInventory", Map.copyOf(inventory));
            file.put("methodCallNames", Map.copyOf(calls));
            file.put("shortCircuitOperators", shortCircuitOperators);
            file.put("selectedMethods", List.copyOf(selectedMethods));
            files.add(Map.copyOf(file));
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    @NonNull
    private Map<String, Object> summarize() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sourceFiles", files.size());
        result.put("declaredTypesInWholeFiles", types.size());
        result.put("declaredOperations", methods.size());
        result.put(
                "physicalLinesSelected",
                files.stream()
                        .mapToInt(row -> ((Number) row.get("selectedPhysicalLines")).intValue())
                        .sum());
        result.put(
                "ncssSelected",
                files.stream()
                        .mapToInt(row -> ((Number) row.get("selectedNcss")).intValue())
                        .sum());
        result.put(
                "sumCyclo",
                methods.stream()
                        .mapToInt(row -> ((Number) row.get("cyclo")).intValue())
                        .sum());
        result.put(
                "sumCognitive",
                methods.stream()
                        .mapToInt(row -> ((Number) row.get("cognitive")).intValue())
                        .sum());
        result.put(
                "shortCircuitOperators",
                files.stream()
                        .mapToInt(row -> ((Number) row.get("shortCircuitOperators")).intValue())
                        .sum());
        Map<String, Long> inventory = new TreeMap<>();
        for (Map<String, Object> file : files) {
            Map<?, ?> nodes = (Map<?, ?>) file.get("astNodeInventory");
            nodes.forEach((name, count) -> inventory.merge(name.toString(), ((Number) count).longValue(), Long::sum));
        }
        result.put("astNodeInventory", Map.copyOf(inventory));
        result.put(
                "cycloOver10",
                methods.stream()
                        .filter(row -> ((Number) row.get("cyclo")).intValue() > 10)
                        .count());
        result.put(
                "cognitiveOver15",
                methods.stream()
                        .filter(row -> ((Number) row.get("cognitive")).intValue() > 15)
                        .count());
        for (String metric : List.of("loc", "ncss", "cyclo", "cognitive", "npath", "fanOut", "arity")) {
            double[] values = methods.stream()
                    .mapToDouble(row -> ((Number) row.get(metric)).doubleValue())
                    .sorted()
                    .toArray();
            result.put(
                    metric,
                    Map.of(
                            "mean",
                            java.util.Arrays.stream(values).average().orElseThrow(),
                            "p50",
                            values[(int) Math.ceil(values.length * 0.50) - 1],
                            "p95",
                            values[(int) Math.ceil(values.length * 0.95) - 1],
                            "max",
                            values[values.length - 1]));
        }
        return Map.copyOf(result);
    }

    private void detectDuplication(Path output) throws Exception {
        Path excerpts = output.resolve("scope-excerpts");
        Files.createDirectories(excerpts);
        for (Map.Entry<Path, ScopeEntry> scoped : scope.entrySet()) {
            ScopeEntry entry = scoped.getValue();
            List<String> lines = Files.readAllLines(scoped.getKey());
            List<Map<String, Object>> selected = methods.stream()
                    .filter(row -> entry.getPath().equals(row.get("path")))
                    .toList();
            List<String> excerpt = new ArrayList<>();
            for (int index = 0; index < lines.size(); index++) {
                int line = index + 1;
                boolean included = entry.isWholeType()
                        || index < 2
                        || selected.stream()
                                .anyMatch(row -> line >= ((Number) row.get("beginLine")).intValue()
                                        && line <= ((Number) row.get("endLine")).intValue());
                excerpt.add(included ? lines.get(index) : "");
            }
            Path destination = excerpts.resolve(entry.getPath());
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, String.join("\n", excerpt) + "\n");
        }
        CPDConfiguration configuration = new CPDConfiguration();
        configuration.setMinimumTileSize(50);
        configuration.setSourceEncoding(StandardCharsets.UTF_8);
        try (CpdAnalysis analysis = CpdAnalysis.create(configuration)) {
            scope.values().forEach(entry -> analysis.files().addFile(excerpts.resolve(entry.getPath())));
            analysis.performAnalysis(report -> {
                if (!report.getProcessingErrors().isEmpty()) {
                    throw new IllegalStateException("CPD errors: " + report.getProcessingErrors());
                }
                try {
                    StringWriter xml = new StringWriter();
                    new XMLRenderer().render(report, xml);
                    String licensed = xml.toString()
                            .replaceFirst(
                                    "\\?>",
                                    "?>\n"
                                            + "<!-- SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>\n"
                                            + "SPDX-License-Identifier: Apache-2.0 -->");
                    Files.writeString(output.resolve("cpd.xml"), licensed);
                    LabFiles.writeJson(
                            output.resolve("cpd-summary.json"),
                            Map.of(
                                    "minimumTokens",
                                    50,
                                    "cloneGroups",
                                    report.getMatches().size(),
                                    "files",
                                    report.getNumberOfTokensPerFile().size(),
                                    "tokenCount",
                                    report.getNumberOfTokensPerFile().values().stream()
                                            .mapToInt(Integer::intValue)
                                            .sum()));
                } catch (java.io.IOException exception) {
                    throw new java.io.UncheckedIOException(exception);
                }
            });
        }
    }

    private static boolean within(Node node, Map<String, Object> method) {
        FileLocation location = node.getTextDocument().toLocation(node.getTextRegion());
        return location.getStartLine() >= ((Number) method.get("beginLine")).intValue()
                && location.getEndLine() <= ((Number) method.get("endLine")).intValue();
    }

    @NonNull
    private static Map<String, Object> common(ScopeEntry entry, Node node) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("path", entry.getPath());
        row.put("role", entry.getRole());
        // PMD method report locations cover the name only. Scope extraction needs the complete
        // text region, otherwise partial shared helpers silently lose their bodies and dependencies.
        FileLocation location = node.getTextDocument().toLocation(node.getTextRegion());
        row.put("beginLine", location.getStartLine());
        row.put("endLine", location.getEndLine());
        return Map.copyOf(row);
    }

    private final class ExportRule extends AbstractJavaRule {
        private ExportRule() {
            setName("PrinterMeasurement");
            setMessage("Export printer measurements");
            setLanguage(LanguageRegistry.PMD.getLanguageById("java"));
        }

        /**
         * Collects one compilation unit without narrowing method bodies to report locations.
         * @param unit parsed source
         * @param data visitor context
         * @return unchanged visitor context
         */
        @Override
        @Nullable
        public Object visit(@NonNull ASTCompilationUnit unit, @Nullable Object data) {
            collect(unit);
            return data;
        }
    }

    /** Identifies complete owned types or explicitly selected shared helper methods. */
    @Value
    @AllArgsConstructor(access = PRIVATE)
    static class ScopeEntry {
        @NonNull
        String role;

        @NonNull
        String path;

        @NonNull
        Set<String> methods;

        /**
         * Identifies complete source-file entries.
         * @return whether every declaration in the source file belongs to the measurement
         */
        boolean isWholeType() {
            return methods.contains("*");
        }

        /**
         * Resolves the source entry to its compiled outer class.
         * @return the primary class named by this source file
         */
        @NonNull
        String resolveClassName() {
            return path.substring(0, path.length() - ".java".length()).replace('/', '.');
        }
    }
}
