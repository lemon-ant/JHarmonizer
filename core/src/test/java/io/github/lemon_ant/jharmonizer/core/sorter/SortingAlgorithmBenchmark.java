package io.github.lemon_ant.jharmonizer.core.sorter;

import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFilesHandler;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonSorter;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.Value;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;

@OutputTimeUnit(java.util.concurrent.TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.AverageTime)
public class SortingAlgorithmBenchmark {

    @Benchmark
    public int benchmarkSortingOnly(BenchmarkState state) {
        int benchmarkChecksum = 0;
        for (BenchmarkFixture benchmarkFixture : state.iterationFixtures) {
            CtCompilationUnit workingCompilationUnit =
                    benchmarkFixture.getCompilationUnitTemplate().clone();
            Set<CtType<?>> skippedTypes =
                    resolveSkippedTypes(workingCompilationUnit, benchmarkFixture.getSortingSkippedTypeQualifiedNames());
            state.spoonSorter.sortCompilationUnitRecursively(workingCompilationUnit, skippedTypes);
            benchmarkChecksum += workingCompilationUnit.getDeclaredTypes().size();
        }
        return benchmarkChecksum;
    }

    @NonNull
    private static Set<CtType<?>> resolveSkippedTypes(
            @NonNull CtCompilationUnit workingCompilationUnit, @NonNull Set<String> skippedQualifiedNames) {
        if (skippedQualifiedNames.isEmpty()) {
            return Set.of();
        }
        Map<String, CtType<?>> typesByQualifiedName = streamTypesRecursively(workingCompilationUnit)
                .collect(Collectors.toMap(CtType::getQualifiedName, Function.identity(), (first, second) -> first));
        return skippedQualifiedNames.stream()
                .map(typesByQualifiedName::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    @NonNull
    private static Stream<CtType<?>> streamTypesRecursively(@NonNull CtCompilationUnit compilationUnit) {
        return compilationUnit.getDeclaredTypes().stream().flatMap(SortingAlgorithmBenchmark::streamTypeTree);
    }

    @NonNull
    private static Stream<CtType<?>> streamTypeTree(@NonNull CtType<?> rootType) {
        Stream<CtType<?>> rootStream = Stream.of(rootType);
        Stream<CtType<?>> nestedStream =
                rootType.getNestedTypes().stream().flatMap(SortingAlgorithmBenchmark::streamTypeTree);
        return Stream.concat(rootStream, nestedStream);
    }

    @State(Scope.Thread)
    public static class BenchmarkState {
        private static final String E2E_REORDER_FIXTURES_ROOT = "/test-cases/core/e2e/reorder/";
        private static final String E2E_REGRESSION_FIXTURES_ROOT = "/test-cases/core/e2e/regression/";

        private SpoonSorter spoonSorter;

        @Param({"1000"})
        private int measurementBatchSize;

        private List<BenchmarkFixture> baseFixtures;
        private List<BenchmarkFixture> iterationFixtures;

        @Setup(Level.Trial)
        public void setUp() {
            CompiledConfig compiledConfig = ConfigurationManager.loadDefaultConfig();
            spoonSorter = new SpoonSorter(compiledConfig);
            List<Path> fixtureRoots = List.of(
                    requireClasspathDirectoryPath(E2E_REORDER_FIXTURES_ROOT),
                    requireClasspathDirectoryPath(E2E_REGRESSION_FIXTURES_ROOT));
            baseFixtures = fixtureRoots.stream()
                    .flatMap(SortingAlgorithmBenchmark::loadFixturesFromRoot)
                    .toList();
            iterationFixtures = baseFixtures;
        }

        @Setup(Level.Iteration)
        public void prepareIterationBatch() {
            if (measurementBatchSize <= 1) {
                iterationFixtures = baseFixtures;
                return;
            }
            iterationFixtures = Stream.generate(() -> baseFixtures)
                    .limit(measurementBatchSize)
                    .flatMap(Collection::stream)
                    .toList();
        }
    }

    @Value
    private static class BenchmarkFixture {
        @NonNull
        CtCompilationUnit compilationUnitTemplate;

        @NonNull
        Set<String> sortingSkippedTypeQualifiedNames;
    }

    @NonNull
    private static Path requireClasspathDirectoryPath(@NonNull String classpathDirectoryPath) {
        try {
            return Path.of(TestCaseResourceUtils.requireClasspathDirectoryUrl(classpathDirectoryPath)
                    .toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "Failed to convert classpath URL to URI: " + classpathDirectoryPath, exception);
        }
    }

    @NonNull
    private static Stream<BenchmarkFixture> loadFixturesFromRoot(@NonNull Path fixtureRoot) {
        return SrcFilesHandler.readJavaFiles(fixtureRoot, List.of("**/input/*.java"), List.of())
                .map(srcFile -> {
                    SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(srcFile);
                    Set<String> sortingSkippedTypeQualifiedNames =
                            spoonAstModel.getOptOuts().getSortingSkippedTypes().stream()
                                    .map(CtType::getQualifiedName)
                                    .collect(Collectors.toUnmodifiableSet());
                    return new BenchmarkFixture(spoonAstModel.getCompilationUnit(), sortingSkippedTypeQualifiedNames);
                });
    }
}
