// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter;

import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.TEST_CASES_DIR;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModelTestCreator.copyWithClonedCompilationUnit;

import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFilesHandler;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonSorter;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.NonNull;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@OutputTimeUnit(java.util.concurrent.TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.AverageTime)
public class SortingAlgorithmBenchmark {

    @Benchmark
    public int benchmarkSortingOnly(@NonNull BenchmarkState state) {
        int benchmarkChecksum = 0;
        for (SpoonAstModel fixtureModel : state.iterationModels) {
            SpoonAstModel workingModel = copyWithClonedCompilationUnit(fixtureModel);
            SpoonAstModel sortedModel = state.spoonSorter.sortCompilationUnitRecursively(workingModel);
            benchmarkChecksum +=
                    sortedModel.getCompilationUnit().getDeclaredTypes().size();
        }
        return benchmarkChecksum;
    }

    @NonNull
    private static Stream<SpoonAstModel> loadFixturesFromRoot(Path fixtureRoot) {
        return SrcFilesHandler.readJavaFiles(fixtureRoot, List.of("**/input/*.java"), List.of())
                .map(srcFile -> SpoonParser.parseJavaSrcFile(srcFile, new PrinterConfig(true, true, false)));
    }

    @NonNull
    private static Path resolveClasspathDirectoryPath(String classpathDirectoryPath) {
        URL directoryUrl = TestCaseResourceUtils.requireClasspathDirectoryUrl(classpathDirectoryPath);
        if (!"file".equals(directoryUrl.getProtocol())) {
            throw new UnsupportedOperationException(
                    "Benchmark fixture scanning requires a file: classpath URL, but got: "
                            + directoryUrl
                            + ". Run this benchmark from an unpackaged Maven test-classes directory.");
        }
        try {
            return Path.of(directoryUrl.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "Failed to convert classpath URL to URI: " + classpathDirectoryPath, exception);
        }
    }

    @State(Scope.Thread)
    public static class BenchmarkState {
        private static final String E2E_REGRESSION_FIXTURES_ROOT = "/" + TEST_CASES_DIR + "/core/e2e/regression/";
        private static final String E2E_REORDER_FIXTURES_ROOT = "/" + TEST_CASES_DIR + "/core/e2e/reorder/";

        private List<SpoonAstModel> baseModels;
        private List<SpoonAstModel> iterationModels;

        @Param({"1000"})
        private int measurementBatchSize;

        private SpoonSorter spoonSorter;

        @Setup(Level.Iteration)
        public void prepareIterationBatch() {
            if (measurementBatchSize <= baseModels.size()) {
                iterationModels = baseModels.subList(0, measurementBatchSize);
                return;
            }
            iterationModels = IntStream.range(0, measurementBatchSize)
                    .mapToObj(fixtureIndex -> baseModels.get(fixtureIndex % baseModels.size()))
                    .toList();
        }

        @Setup(Level.Trial)
        public void setUp() {
            CompiledConfig compiledConfig = ConfigurationManager.loadDefaultConfig();
            spoonSorter = new SpoonSorter(compiledConfig);
            List<Path> fixtureRoots = List.of(
                    resolveClasspathDirectoryPath(E2E_REORDER_FIXTURES_ROOT),
                    resolveClasspathDirectoryPath(E2E_REGRESSION_FIXTURES_ROOT));
            baseModels = fixtureRoots.stream()
                    .flatMap(SortingAlgorithmBenchmark::loadFixturesFromRoot)
                    .toList();
            iterationModels = baseModels;
        }
    }
}
