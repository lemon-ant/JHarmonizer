// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.printerlab;

import com.fasterxml.jackson.databind.JsonNode;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.cycle_detection.CycleDetector;
import com.tngtech.archunit.library.cycle_detection.Cycles;
import com.tngtech.archunit.library.cycle_detection.Edge;
import com.tngtech.archunit.library.metrics.LakosMetrics;
import com.tngtech.archunit.library.metrics.MetricsComponents;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Measures only declared project dependencies; external class bodies are never imported. */
@UtilityClass
final class ArchitectureMetrics {

    /**
     * Records declared dependencies and standard class-graph metrics.
     * @param repository checkout root
     * @param output run directory
     * @param scope selected source declarations
     */
    static void measure(@NonNull Path repository, @NonNull Path output, @NonNull List<SourceMetrics.ScopeEntry> scope)
            throws Exception {
        ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
        JavaClasses imported = new ClassFileImporter().importPath(repository.resolve("core/target/classes"));
        Map<String, SourceMetrics.ScopeEntry> entries = scope.stream()
                .collect(Collectors.toUnmodifiableMap(SourceMetrics.ScopeEntry::resolveClassName, Function.identity()));
        Map<String, SourceMetrics.ScopeEntry> selected = new TreeMap<>();
        for (JavaClass type : imported) {
            String outerName = type.getName().split("\\$")[0];
            if (entries.containsKey(outerName)) {
                selected.put(type.getName(), entries.get(outerName));
            }
        }
        if (!selected.keySet().containsAll(entries.keySet())) {
            throw new IllegalStateException("Compiled classes do not match source scope; rebuild core");
        }
        JsonNode sources = LabFiles.readJson(output.resolve("source-metrics.json"));
        Map<String, Set<String>> graph = new TreeMap<>();
        selected.keySet().forEach(name -> graph.put(name, new TreeSet<>()));
        List<Map<String, Object>> dependencies = new ArrayList<>();
        List<Map<String, Object>> classes = new ArrayList<>();
        Set<String> unscoped = new TreeSet<>();
        Set<String> externalTargets = new TreeSet<>();
        for (Map.Entry<String, SourceMetrics.ScopeEntry> selection : selected.entrySet()) {
            JavaClass type = imported.get(selection.getKey());
            SourceMetrics.ScopeEntry entry = selection.getValue();
            Set<String> targets = new TreeSet<>();
            for (Dependency dependency : type.getDirectDependenciesFromSelf()) {
                int line = dependency.getSourceCodeLocation().getLineNumber();
                if (!entry.isWholeType() && !isSelectedLine(sources, entry.getPath(), line)) {
                    continue;
                }
                JavaClass targetClass = dependency.getTargetClass().getBaseComponentType();
                if (targetClass.isPrimitive()) {
                    continue;
                }
                String target = targetClass.getName();
                if (target.equals(type.getName())) {
                    continue;
                }
                targets.add(target);
                String category = category(target, selected.keySet());
                dependencies.add(Map.of(
                        "from",
                        type.getName(),
                        "to",
                        target,
                        "category",
                        category,
                        "line",
                        line,
                        "description",
                        dependency.getDescription()));
                if (selected.containsKey(target)) {
                    graph.get(type.getName()).add(target);
                } else if (target.startsWith("io.github.lemon_ant.jharmonizer.")) {
                    unscoped.add(target);
                } else if (!category.equals("jdk")) {
                    externalTargets.add(target);
                }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("class", type.getName());
            row.put("role", entry.getRole());
            row.put("partialHelper", !entry.isWholeType());
            row.put("distinctDependencyTargets", targets.size());
            row.put("ownFieldsInWholeClass", type.getFields().size());
            row.put(
                    "nonFinalFieldsInWholeClass",
                    type.getFields().stream()
                            .filter(field -> !field.getModifiers().contains(JavaModifier.FINAL))
                            .count());
            row.put("declaredBytecodeMethodsInWholeClass", type.getMethods().size());
            row.put(
                    "directSuperclass",
                    type.getRawSuperclass().map(JavaClass::getName).orElse(""));
            row.put(
                    "externalNonObjectSuperclass",
                    type.getRawSuperclass()
                            .filter(parent -> !parent.getName().equals("java.lang.Object")
                                    && !selected.containsKey(parent.getName()))
                            .map(JavaClass::getName)
                            .orElse(""));
            classes.add(row);
        }
        List<Edge<String>> edges = graph.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(target -> Edge.create(entry.getKey(), target)))
                .toList();
        Cycles<Edge<String>> cycles = CycleDetector.detectCycles(selected.keySet(), edges);
        Map<String, Set<String>> immutableGraph = graph.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
        LakosMetrics lakos = com.tngtech.archunit.library.metrics.ArchitectureMetrics.lakosMetrics(
                MetricsComponents.from(List.copyOf(selected.keySet()), Function.identity()), immutableGraph::get);
        for (Map<String, Object> row : classes) {
            String name = row.get("class").toString();
            row.put("internalFanOut", graph.get(name).size());
            row.put(
                    "internalFanIn",
                    graph.values().stream()
                            .filter(targets -> targets.contains(name))
                            .count());
        }
        dependencies.sort(Comparator.comparing(
                row -> row.get("from") + ":" + row.get("to") + ":" + row.get("line") + ":" + row.get("description")));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tool", "ArchUnit 1.5.0; missing dependency resolution disabled");
        result.put("classes", classes.stream().map(Map::copyOf).toList());
        result.put("dependencies", List.copyOf(dependencies));
        result.put("unscopedProjectTargets", List.copyOf(unscoped));
        result.put("distinctExternalLibraryTargets", List.copyOf(externalTargets));
        result.put("internalEdges", edges.size());
        result.put("cycleCount", cycles.size());
        result.put("cycleEnumerationTruncated", cycles.maxNumberOfCyclesReached());
        result.put("cycles", cycles.stream().map(Object::toString).toList());
        result.put("CCD", lakos.getCumulativeComponentDependency());
        result.put("ACD", lakos.getAverageComponentDependency());
        result.put("RACD", lakos.getRelativeAverageComponentDependency());
        result.put("NCCD", lakos.getNormalizedCumulativeComponentDependency());
        LabFiles.writeJson(output.resolve("architecture.json"), Map.copyOf(result));
        StringBuilder diagram = new StringBuilder("# Printer dependency graph\n\n"
                + "Declared bytecode dependencies within the measured scope.\n\n"
                + "```mermaid\n"
                + "graph TD\n");
        selected.keySet()
                .forEach(name -> diagram.append("  ")
                        .append(nodeId(name))
                        .append("[\"")
                        .append(name.substring(name.lastIndexOf('.') + 1))
                        .append("\"]\n"));
        edges.forEach(edge -> diagram.append("  ")
                .append(nodeId(edge.getOrigin()))
                .append(" --> ")
                .append(nodeId(edge.getTarget()))
                .append('\n'));
        diagram.append("```\n");
        LabFiles.writeMarkdown(output.resolve("architecture.md"), diagram.toString());
        if (!unscoped.isEmpty()) {
            throw new IllegalStateException(
                    "Printer references unmeasured project code; review scope.tsv: " + unscoped);
        }
    }

    private static boolean isSelectedLine(JsonNode sources, String path, int line) {
        for (JsonNode method : sources.path("methods")) {
            if (path.equals(method.path("path").asText())
                    && line >= method.path("beginLine").asInt()
                    && line <= method.path("endLine").asInt()) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static String category(String target, Set<String> selected) {
        if (selected.contains(target)) {
            return "printer";
        }
        if (target.startsWith("io.github.lemon_ant.jharmonizer.")) {
            return "unscoped-project";
        }
        return target.startsWith("java.") || target.startsWith("javax.") || target.startsWith("jdk.")
                ? "jdk"
                : "library";
    }

    @NonNull
    private static String nodeId(String name) {
        return name.replace('.', '_').replace('$', '_');
    }
}
