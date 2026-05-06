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
`io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector`, with
the LIS computation factored into
`io.github.lemon_ant.jharmonizer.core.translator.spoon.LongestIncreasingSubsequenceUtils`.

## Inputs

- The original DFS source-order snapshot of `CtTypeMember`s, captured at parse time
  by `RelocationDetector.snapshotOriginalMemberOrder(...)` and stored on
  `SpoonAstModel.originalMemberOrder` (built via
  `SpoonTypeUtils.streamDeclaredHierarchy`).
- The post-sort AST produced by the sorter — i.e. the same Spoon model with
  members reordered in place.

## Output

A list of `MemberRelocation` records. Each record represents one contiguous run of
moved members in the **sorted** order, and carries:

- `movedMembers` — the run of members, in their final order;
- `predecessor` / `successor` — the sorted-order neighbours that frame the run
  (either may be `null` when the run sits at the start or end of its scope).

`MemberRelocationPrinter` turns each record into one line of the form
"move *N* members before *X*", so the user sees one diagnostic line per
contiguous run instead of one line per moved member.

## Algorithm

The detector runs once per *scope* (file root, then each type body it contains):

1. **Build a per-scope successor map.**
   `RelocationDetector.buildScopeSuccessorMap(...)` walks the original DFS snapshot
   and derives the per-scope `Map<CtTypeMember, CtTypeMember>` of consecutive pairs.
2. **Project the sorted members onto original-source indices.**
   For each member in the sorted order, look up its index in
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
- **Diagnostic-only.** The detector's output never feeds back into sorting,
  serialization, formatting, or check decisions. Disabling it would not change a
  single byte of produced source.
- **Resilient to missing positions.** Members without a valid source position
  (synthetic/implicit members, members the parser could not pin to a region) are
  tagged `UNTRACKED` and treated as stable. They are silently ignored by the
  report rather than being mis-reported as moved.
