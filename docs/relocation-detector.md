<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Relocation detector

The relocation detector turns the **before/after** member orders produced by the
sorter into a compact, human-friendly *relocation report*. It does not change any
output — it is a pure diagnostic pass whose result is printed by
`MemberRelocationPrinter`.

The implementation lives in
`io.github.lemon_ant.jharmonizer.core.spoon.RelocationDetector`, with
the LIS computation factored into
`io.github.lemon_ant.jharmonizer.core.spoon.LongestIncreasingSubsequenceUtils`.

`SpoonSorter` emits declaration and annotation change flags while collecting final output. Element identities are
compared against input order during the same traversal; checks stop after the first mismatch in each scope. Grouping
and dependency repair complete before the flag is decided, so temporary moves that cancel out do not report a change.
Annotation groups remain in source-range order. Preparing them collects whether any group's annotation order changed;
it does not compare group permutations.
`SortingResult.annotationsReordered` controls annotation diffs. The independent `membersReordered` flag reports Spoon
declaration changes, including top-level types. `isReordered()` derives their OR for flow status and sorting checks.

`Sorter.sort(...)` prepares the LIS member report only when requested and declaration order changed. Check flows request
and consume `SortingResult.memberRelocations`; `ReorderFlow` requests only the change flags. Annotation-only changes and
unchanged declarations skip the diagnostic pass. Sorting and requested diagnostics share one timer. The LIS pass remains
separate because intermediate sorting operations do not define a minimal relocation report.

The combined flag is independent of member diagnostics, which omit untracked nodes and invalid source positions.
Each freshly parsed shared AST is sorted once per flow, so invocation changes also describe differences from source
order. The parse-time snapshot remains necessary for detailed reports. `SpoonSorter` claims the shared compilation unit
before sorting and rejects repeated attempts through any model view, including after unchanged or failed sorting.
Clones retain a consumed unit's claim; reparsing starts a fresh lifecycle. Skipped sorting returns empty diagnostics and
`false` change flags.

## Inputs

- The original DFS source-order snapshot of `CtTypeMember`s, captured at parse time
  by `RelocationDetector.snapshotOriginalMemberOrder(...)` and stored on
  `SpoonAstModel.originalMemberOrder` (built via
  `SpoonTypeUtils.streamDeclaredHierarchy`).
- The post-sort AST produced by the sorter — i.e. the same Spoon model with
  members reordered in place.

## Output

A list of `MemberRelocation` values. Each value represents one contiguous run of
moved members in the **sorted** order, and carries:

- `relocatedMembers` — the run of members, in their final order;
- `sortedPredecessor` / `sortedSuccessor` — the sorted-order neighbours that frame the run
  (either may be `null` when the run sits at the start or end of its scope).

The result list and each moved chunk are unmodifiable views without defensive copies. Producers must not modify
handed-off lists. The detector builds fresh result and scope lists for each report. The referenced Spoon nodes remain
mutable.

`MemberRelocationPrinter` turns each value into one line of the form
"move *N* members before *X*", so the user sees one diagnostic line per
contiguous run instead of one line per moved member.

## Algorithm

The detector builds one index, then visits the file root and each type body:

1. **Build an original-order index.**
   `RelocationDetector.buildOriginalIndexMap(...)` maps tracked members to their positions in the original DFS snapshot.
   It uses node identity so structurally equal declarations in different scopes remain distinct.
2. **Project each sorted scope onto original-source indices.**
   For each member in the scope's sorted order, look up its index in
   `originalMemberOrder`; members with invalid source positions, or members not
   present in the snapshot, are tagged with the `UNTRACKED = -1` sentinel and
   treated as stable.
3. **Compute the Longest Increasing Subsequence (LIS).**
   `LongestIncreasingSubsequenceUtils.computeLisMask(...)` runs a patience-sort
   variant over the index array and returns a boolean mask of LIS membership.
   Members in the LIS are those that already appear in the same relative order in
   both the original and the sorted lists, and are therefore **stable**.
4. **Take the complement.** Members not in the LIS form the **minimal** set of
   members that must move to transform the original order into the sorted order.
   This is a classical reduction: the LIS gives the largest order-preserving
   subsequence, so its complement is the smallest moved set.
5. **Glue contiguous runs.** Adjacent moved members in the sorted order are merged
   into a single `MemberRelocation` chunk so that the report shows one diagnostic
   line per run.
6. **Annotate boundaries.** The chunk's `predecessor` is the member immediately
   before the run in the sorted order; its `successor` is the member immediately
   after. Either may be `null` at scope boundaries.

## Why LIS

The pair *(original order, sorted order)* defines a permutation. The minimum number
of elements that must be moved to transform one to the other is exactly
*N − |LIS|*, where the LIS is taken over the original-source indices in the sorted
order. Reporting the LIS complement is therefore the *smallest* set of moves the
user has to read about — anything larger would either describe redundant moves or
mis-attribute movement to stable members.

## Properties

- **Deterministic.** The patience-sort LIS is deterministic for a fixed input;
  ties are broken by index.
- **Diagnostic-only.** The report does not alter declaration order or printed source. Check flows use its presence to
  classify member-ordering violations and skip formatting until sorting passes.
- **Resilient to missing positions.** Members without a valid source position
  (synthetic/implicit members, members the parser could not pin to a region) are
  tagged `UNTRACKED` and treated as stable. They are silently ignored by the
  report rather than being mis-reported as moved.
