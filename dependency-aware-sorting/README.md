# dependency-aware-sorting

A Java 17+ research project (compatible with Java 21) that implements a **fast, deterministic algorithm** for
ordering `SortableTypeMember` objects under two kinds of constraints:

1. **Groups** — members in the same group form an indivisible block and always appear
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
| `SimplifiedDependencyAwareSorter` | Lombok `@UtilityClass` — high-performance variant requiring groups and dependencies to be mutually exclusive |
| `SortingException` | Thrown on invalid input (cycle, duplicate name, member in two groups, …) |

### Public API

```java
// Default comparator (STATIC first by name, then DYNAMIC by name)
List<SortableTypeMember> result = SimplifiedDependencyAwareSorter.sort(members, groups, dependencies,
        SortableTypeMember.DEFAULT_ORDER);

// Custom comparator overload
List<SortableTypeMember> result = SimplifiedDependencyAwareSorter.sort(members, groups, dependencies,
        Comparator.comparing(m -> m.getOrderingKey().getName()));
```

### Quick example

```java
SortableTypeMember delta = SortableTypeMember.staticMember("delta");
SortableTypeMember echo  = SortableTypeMember.staticMember("echo");
SortableTypeMember alpha = SortableTypeMember.staticMember("alpha");
SortableTypeMember beta  = SortableTypeMember.staticMember("beta");

List<SortableTypeMember> result = SimplifiedDependencyAwareSorter.sort(
    List.of(alpha, beta, delta, echo),
    Groups.of(
        Group.of(delta, echo),   // {delta, echo} travel together
        Group.of(alpha, beta)),  // {alpha, beta} travel together
    Dependencies.of(delta, beta), // group {delta,echo} must precede {alpha,beta}
    SortableTypeMember.DEFAULT_ORDER
);
// → [delta, echo, alpha, beta]
```

### Default ordering

`SortableTypeMember.DEFAULT_ORDER` places `STATIC` members before `DYNAMIC`, each group
sorted by name:

```java
SimplifiedDependencyAwareSorter.sort(List.of(
    SortableTypeMember.dynamicMember("bravo"),
    SortableTypeMember.staticMember("charlie"),
    SortableTypeMember.dynamicMember("alpha"),
    SortableTypeMember.staticMember("delta")),
    Groups.empty(), Dependencies.empty(),
    SortableTypeMember.DEFAULT_ORDER);
// → [charlie, delta, alpha, bravo]
//    ────STATIC────   ──DYNAMIC──
```

---

## Ordering rules

1. **No constraints** — members are ordered by the supplied comparator (default: `STATIC` first by name, then `DYNAMIC` by name).
2. **Group** — members in a group travel together as an indivisible block.
   - Within the block: comparator order.
   - Block position among all blocks: determined by the block's **key** (comparator-minimum member in the block), unless dependencies force a different position.
3. **Dependency `provider → dependent`** — the provider's block must appear before the dependent's block; overrides natural comparator order.
4. **Group–dependency mutual exclusivity** (`SimplifiedDependencyAwareSorter` precondition) — a member that belongs to a group must not appear in any dependency (as provider or dependent); violating this raises `SortingException`.
5. **Cycle** — a cycle in the dependency graph raises `SortingException`.
6. **Duplicate names** — two members with the same name raise `SortingException`.
7. **Member in two groups** — raises `SortingException`.

---

## Algorithm

### Overview

```
Input items
    │
    ▼
1. Validate (unique names, single-group membership,
             groups and dependencies are mutually exclusive)
    │
    ▼
2. Map items → compact int indices
   Assign super-nodes:
     • each group     → one super-node (members sorted by comparator inside)
     • each singleton → one super-node
    │
    ▼
3. Build directed graph on super-nodes from dependency edges
    │
    ▼
4. Compute base order (comparator sort of super-nodes)
   Repair with provider-lift:
     scan base order left to right; when a blocked dependent is encountered,
     lift its minimal transitive provider closure as a contiguous block
     directly before it (providers topologically sorted among themselves
     with base-rank tie-breaking)
    │
    ▼
5. Expand super-nodes → final item list
```

### Key design decisions

- **Flat-array super-node storage** — a single `int[n]` holds all item indices grouped by
  super-node with offset/length pairs for O(1) random access; eliminates `List<Integer>` boxing.
- **Fastutil primitives** — `IntList`, `IntHeapPriorityQueue`, `Object2IntOpenHashMap` eliminate
  all `Integer` boxing in the hot path.
- **Compact adjacency storage** via fastutil `IntList` reduces allocation and boxing for outgoing edges.
- **Pluggable comparator** — the sort algorithm is fully decoupled from the ordering
  rules; pass any `Comparator<T>` or rely on `SortableTypeMember.DEFAULT_ORDER`.
- **Fast path** — when there are no groups or dependencies, items are sorted directly
  by the comparator with a single `List.sort()`, bypassing all super-node machinery.

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

**`AbstractDependencyAwareSortingTest`** (shared base, exercised via `SimplifiedDependencyAwareSorterTest`):
- Plain sorting with `DEFAULT_ORDER` (STATIC before DYNAMIC, each group by name)
- Custom comparator overload
- Group handling, including single and multiple groups
- Dependency ordering, including transitive and diamond-shaped graphs
- Error cases such as cycles, duplicate names, invalid group membership
- Determinism checks across input permutations
- Large-input sanity/performance-oriented scenarios

**`SimplifiedDependencyAwareSorterTest`** — adds validation tests specific to the mutual-exclusivity constraint (group member used as provider or dependent raises `SortingException`).

**`GenericSortingTest`** — exercises the API with plain `String` and `Integer` items to verify true generalization.

**`JsonDrivenSortingTest`** (integration tests from JSON files, count grows as cases are added):

Each test case lives in `src/test/resources/cases/<name>/` with two files:

| File | Contents |
|------|----------|
| `input.json` | `description`, `items` (array of `{"name": "...", "numeration": "STATIC\|DYNAMIC"}`), `clusters` (JSON key for groups), `dependencies` |
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
Complex mix: three groups, four singletons...
  Input items : [zulu:STATIC, yankee:STATIC, xray:STATIC, whiskey:STATIC, ...]
  Groups      : [[zulu, xray], [whiskey, victor], ...]
  Dependencies: [xray->victor, whiskey->sierra, romeo->papa]
  Expected    : [quebec, romeo, papa, xray, zulu, ...]
  Actual (simplified): [quebec, romeo, papa, xray, zulu, ...]
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
