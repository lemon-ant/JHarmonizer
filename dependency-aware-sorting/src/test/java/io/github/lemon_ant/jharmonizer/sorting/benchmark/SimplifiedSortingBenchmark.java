package io.github.lemon_ant.jharmonizer.sorting.benchmark;

import io.github.lemon_ant.jharmonizer.sorting.Dependencies;
import io.github.lemon_ant.jharmonizer.sorting.Group;
import io.github.lemon_ant.jharmonizer.sorting.Groups;
import io.github.lemon_ant.jharmonizer.sorting.SimplifiedDependencyAwareSorter;
import io.github.lemon_ant.jharmonizer.sorting.SortableTypeMember;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for {@link SimplifiedDependencyAwareSorter}
 * on inputs that satisfy the simplified preconditions (no group–dependency overlap).
 *
 * <p>Run with:
 * <pre>
 *   mvn test-compile exec:java
 * </pre>
 *
 * <p>Scenarios use the same item counts and similar numbers of groups/edges, but the data is
 * generated so that groups and dependencies reference disjoint sets of members.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Threads(4)
@Fork(0)
public class SimplifiedSortingBenchmark {

    // ------------------------------------------------------------------ //
    // Shared (read-only) state                                            //
    // ------------------------------------------------------------------ //

    // --- 50 items, no constraints ---
    List<SortableTypeMember> items50;
    Groups<SortableTypeMember> groups50Empty;
    Dependencies<SortableTypeMember> deps50Empty;

    // --- 50 items, with groups + deps (no overlap) ---
    List<SortableTypeMember> items50c;
    Groups<SortableTypeMember> groups50C;
    Dependencies<SortableTypeMember> deps50c;

    // --- 500 items, with groups + deps (no overlap) ---
    List<SortableTypeMember> items500;
    Groups<SortableTypeMember> groups500;
    Dependencies<SortableTypeMember> deps500;

    // --- 5000 items, with groups + deps (no overlap) ---
    List<SortableTypeMember> items5000;
    Groups<SortableTypeMember> groups5000;
    Dependencies<SortableTypeMember> deps5000;

    @Setup(Level.Trial)
    public void setup() {
        // Scenario 1 — baseline (identical for all algorithms)
        items50 = makeItems(50);
        groups50Empty = Groups.empty();
        deps50Empty = Dependencies.empty();

        // Scenario 2 — 50 items, 12 groups of 2 = 24 grouped, 26 non-grouped
        items50c = makeItems(50);
        int cl50 = items50c.size() / 2 / 2; // 12 groups
        groups50C = makeSimplifiedGrouping(items50c, cl50, 2);
        deps50c = makeSimplifiedDeps(items50c, 20, cl50 * 2);

        // Scenario 3 — 500 items, 125 groups of 2 = 250 grouped
        items500 = makeItems(500);
        int cl500 = items500.size() / 2 / 2; // 125 groups
        groups500 = makeSimplifiedGrouping(items500, cl500, 2);
        deps500 = makeSimplifiedDeps(items500, 200, cl500 * 2);

        // Scenario 4 — 5000 items, 1250 groups of 2 = 2500 grouped
        items5000 = makeItems(5000);
        int cl5000 = items5000.size() / 2 / 2; // 1250 groups
        groups5000 = makeSimplifiedGrouping(items5000, cl5000, 2);
        deps5000 = makeSimplifiedDeps(items5000, 1000, cl5000 * 2);
    }

    // ------------------------------------------------------------------ //
    // Benchmarks – SimplifiedDependencyAwareSorter                        //
    // ------------------------------------------------------------------ //

    @Benchmark
    public void simplified_50_noConstraints(Blackhole bh) {
        bh.consume(SimplifiedDependencyAwareSorter.sort(
                items50, groups50Empty, deps50Empty, SortableTypeMember.DEFAULT_ORDER));
    }

    @Benchmark
    public void simplified_50_withConstraints(Blackhole bh) {
        bh.consume(
                SimplifiedDependencyAwareSorter.sort(items50c, groups50C, deps50c, SortableTypeMember.DEFAULT_ORDER));
    }

    @Benchmark
    public void simplified_500_withConstraints(Blackhole bh) {
        bh.consume(
                SimplifiedDependencyAwareSorter.sort(items500, groups500, deps500, SortableTypeMember.DEFAULT_ORDER));
    }

    @Benchmark
    public void simplified_5000_withConstraints(Blackhole bh) {
        bh.consume(SimplifiedDependencyAwareSorter.sort(
                items5000, groups5000, deps5000, SortableTypeMember.DEFAULT_ORDER));
    }

    // ------------------------------------------------------------------ //
    // Data generators                                                     //
    // ------------------------------------------------------------------ //

    /** Creates {@code n} STATIC members in reverse name order (forces real sorting work). */
    private static List<SortableTypeMember> makeItems(int n) {
        return IntStream.iterate(n - 1, i -> i - 1)
                .limit(n)
                .mapToObj(i -> SortableTypeMember.staticMember(String.format("item%05d", i)))
                .collect(Collectors.toList());
    }

    /**
     * Creates groups from the <em>first</em> {@code numGroups * groupSize} members,
     * leaving the rest available for dependency edges (no overlap).
     */
    private static Groups<SortableTypeMember> makeSimplifiedGrouping(
            List<SortableTypeMember> allItems, int numGroups, int groupSize) {
        List<Group<SortableTypeMember>> groups = IntStream.range(0, numGroups)
                .mapToObj(groupIndex ->
                        new Group<>(allItems.subList(groupIndex * groupSize, groupIndex * groupSize + groupSize)))
                .toList();
        return new Groups<>(groups);
    }

    /**
     * Creates a DAG with up to {@code edgeCount} acyclic provider→dependent edges, using only
     * members from index {@code firstFreeIdx} onward (i.e. non-grouped members).
     */
    private static Dependencies<SortableTypeMember> makeSimplifiedDeps(
            List<SortableTypeMember> allItems, int edgeCount, int firstFreeIdx) {
        int totalSize = allItems.size();
        int available = totalSize - firstFreeIdx;
        int stride = Math.max(1, available / edgeCount);

        List<Dependencies.Dependency<SortableTypeMember>> edges = IntStream.range(0, edgeCount)
                .filter(i -> {
                    int provIdx = totalSize - 1 - i;
                    int depIdx = provIdx - stride;
                    return provIdx >= firstFreeIdx && depIdx >= firstFreeIdx;
                })
                .mapToObj(i -> new Dependencies.Dependency<>(
                        allItems.get(totalSize - 1 - i), allItems.get(totalSize - 1 - i - stride)))
                .toList();
        return new Dependencies<>(edges);
    }
}
