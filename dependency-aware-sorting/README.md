# dependency-aware-sorting

A Java 17+ research project (compatible with Java 21) that implements a **fast, deterministic algorithm** for
ordering `SortableTypeMember` objects under two kinds of constraints:

1. **Clusters** — members in the same cluster form an indivisible block and always appear
   together in the output, internally ordered by a pluggable comparator.
2. **Dependencies** — `provider → dependent` DAG edges require the provider's block to appear
   before the dependent's block in the final order.

The baseline natural order (used as a tie-breaker when no constraint decides the position)
is `SortableTypeMember.DEFAULT_ORDER`: all `STATIC` members first (sorted by name), then all
`DYNAMIC` members (sorted by name).

---

## Domain model

Domain objects are implemented with **Lombok `@Value`** (immutable value classes with
all-args constructor, getters, `equals`, `hashCode`, and `toString` generated at compile time).

| Type | Role |
|------|------|
| `SortableTypeMember` | `@Value` — sortable container holding an `OrderingKey` (`getOrderingKey()`, `getName()`) |
| `SortableTypeMember.OrderingKey` | `@Value` — composite sort key: `String name` + `Numeration` enum (`STATIC`/`DYNAMIC`) |
| `SortableTypeMember.Numeration` | Enum — `STATIC` or `DYNAMIC`; controls the default ordering tier |
| `Group` | `@Value` — an indivisible group holding `List<SortableTypeMember>` (`getItems()`) |
| `Groups` | `@Value` — holds `List<Group>` (`getGroups()`); factory: `Groups.of(Group...)` |
| `Dependencies` | `@Value` — holds `List<Dependency>` (`getEdges()`) |
| `Dependencies.Dependency` | `@Value` — a single `provider → dependent` ordering edge |
| `DependencyAwareSorter` | Lombok `@UtilityClass` — general algorithm, supports group+dependency overlap |
| `SimplifiedDependencyAwareSorter` | Lombok `@UtilityClass` — optimised variant requiring groups and dependencies to be mutually exclusive |
| `SortingException` | Thrown on invalid input (cycle, duplicate name, member in two groups, …) |

### Public API

```java
// Default comparator (STATIC first by name, then DYNAMIC by name)
List<SortableTypeMember> result = DependencyAwareSorter.sort(members, groups, dependencies,
        SortableTypeMember.DEFAULT_ORDER);

// Simplified variant (groups and dependencies must not overlap)
List<SortableTypeMember> result = SimplifiedDependencyAwareSorter.sort(members, groups, dependencies,
        SortableTypeMember.DEFAULT_ORDER);

// Custom comparator overload
List<SortableTypeMember> result = DependencyAwareSorter.sort(members, groups, dependencies,
        Comparator.comparing(m -> m.getOrderingKey().getName()));
```

### Quick example

```java
SortableTypeMember delta = SortableTypeMember.staticMember("delta");
SortableTypeMember echo  = SortableTypeMember.staticMember("echo");
SortableTypeMember alpha = SortableTypeMember.staticMember("alpha");
SortableTypeMember beta  = SortableTypeMember.staticMember("beta");

List<SortableTypeMember> result = ConstrainedSorter.sort(
    List.of(alpha, beta, delta, echo),
    Clustering.of(
        Cluster.of(delta, echo),   // {delta, echo} travel together
        Cluster.of(alpha, beta)),  // {alpha, beta} travel together
    Dependencies.of(delta, beta)   // cluster {delta,echo} must precede {alpha,beta}
);
// → [delta, echo, alpha, beta]
```

### Default ordering

`SortableTypeMember.DEFAULT_ORDER` places `STATIC` members before `DYNAMIC`, each group
sorted by name:

```java
ConstrainedSorter.sort(List.of(
    SortableTypeMember.dynamicMember("bravo"),
    SortableTypeMember.staticMember("charlie"),
    SortableTypeMember.dynamicMember("alpha"),
    SortableTypeMember.staticMember("delta")),
    Clustering.EMPTY, Dependencies.EMPTY);
// → [charlie, delta, alpha, bravo]
//    ────STATIC────   ──DYNAMIC──
```

---

## Ordering rules

1. **No constraints** — members are ordered by the supplied comparator (default: `STATIC` first by name, then `DYNAMIC` by name).
2. **Cluster** — members in a cluster travel together as an indivisible block.
   - Within the block: comparator order.
   - Block position among all blocks: determined by the block's **key** (comparator-minimum member in the block), unless dependencies force a different position.
3. **Dependency `provider → dependent`** — the provider's block must appear before the dependent's block; overrides natural comparator order.
4. **Intra-cluster dependency** — if provider and dependent are in the *same* cluster, the dependency is only valid when the comparator already places the provider before the dependent. A conflicting intra-cluster dependency raises `SortingException`.
5. **Cycle** — a cycle in the dependency graph raises `SortingException`.
6. **Duplicate names** — two members with the same name raise `SortingException`.
7. **Member in two clusters** — raises `SortingException`.

---

## Algorithm

### Overview

```
Input members
    │
    ▼
1. Validate (unique names, single-cluster membership)
    │
    ▼
2. Map members → compact int indices
   Assign super-nodes:
     • each cluster   → one super-node (members sorted by comparator inside)
     • each singleton → one super-node
    │
    ▼
3. Build directed graph on super-nodes from inter-cluster dependency edges
   (intra-cluster deps are validated but need no graph edge)
    │
    ▼
4. Kahn's topological sort with comparator-based tie-breaking
   (at each step pick the ready super-node with the smallest comparator key)
    │
    ▼
5. Expand super-nodes → final member list
```

### Key design decisions

- **`List<Integer>` collections** are retained for core internal state for readability and consistency.
- **Compact adjacency storage** via an internal int bag reduces boxing overhead for outgoing edges while keeping the public design unchanged.
- **`PriorityQueue`** ordered by super-node key gives O(log S) per dequeue where
  S ≤ n is the number of super-nodes.
- **Pluggable comparator** — the sort algorithm is fully decoupled from the ordering
  rules; pass any `Comparator<SortableTypeMember>` or rely on `DEFAULT_ORDER`.
- No third-party production dependencies.

### Complexity

| Measure | Value |
|---------|-------|
| Time | O(n log n + E) — n members, E dependency edges |
| Space | O(n + E) |

---

## Running the tests

```bash
mvn test
```

All tests are in `src/test/java/io/github/lemon_ant/jharmonizer/sorting/` and cover:

**`AbstractDependencyAwareSortingTest`** (shared base, exercised via `DependencyAwareSorterTest` and `SimplifiedDependencyAwareSorterTest`):
- Plain sorting with `DEFAULT_ORDER` (STATIC before DYNAMIC, each group by name)
- Custom comparator overload
- Group handling, including single and multiple groups
- Dependency ordering, including transitive and diamond-shaped graphs
- Error cases such as cycles, duplicate names, invalid group membership
- Determinism checks across input permutations
- Large-input sanity/performance-oriented scenarios

**`JsonDrivenSortingTest`** (integration tests from JSON files, count grows as cases are added):

Each test case lives in `src/test/resources/cases/<name>/` with two files:

| File | Contents |
|------|----------|
| `input.json` | `description`, `items` (array of `{"name": "...", "numeration": "STATIC\|DYNAMIC"}`), `clusters`, `dependencies` |
| `expected.json` | Expected sorted list of item names |

Example `input.json` fragment:
```json
{
  "description": "...",
  "items": [
    {"name": "alpha", "numeration": "STATIC"},
    {"name": "beta",  "numeration": "DYNAMIC"}
  ],
  "clusters": [["alpha"]],
  "dependencies": [{"provider": "alpha", "dependent": "beta"}]
}
```

Running the test prints a readable table for each case:
```
=== 10-complex-mix ===
Complex mix: three clusters, four singletons...
  Input items : [zulu:STATIC, yankee:STATIC, xray:STATIC, whiskey:STATIC, ...]
  Clusters    : [[zulu, xray], [whiskey, victor], ...]
  Dependencies: [xray->victor, whiskey->sierra, romeo->papa]
  Expected    : [quebec, romeo, papa, xray, zulu, ...]
  Actual      : [quebec, romeo, papa, xray, zulu, ...]
```

To **add a new test case** simply create a new sub-directory under `cases/` with the
two JSON files — no code changes required.

---

## Running benchmarks

```bash
mvn test-compile exec:java
```

Runs [JMH](https://github.com/openjdk/jmh) benchmarks (10 measurement iterations × 1 s,
4 parallel threads) across four scenarios and writes results to `benchmark-results.json`.

Benchmark scenarios in `SortingBenchmark`:

| Benchmark | Members | Constraints |
|-----------|---------|-------------|
| `sort_50_noConstraints` | 50 | none |
| `sort_50_withConstraints` | 50 | 5 clusters × 5 + 30 dep edges |
| `sort_500_withConstraints` | 500 | 25 clusters × 10 + 200 dep edges |
| `sort_5000_withConstraints` | 5 000 | 125 clusters × 20 + 1 000 dep edges |

---

## Requirements

- Java 17+ (no Java-21-specific syntax is used, so the project compiles and runs on
  Java 17 as well as Java 21)
- Maven 3.8+
