# JHarmonizer — backlog of design & performance ideas

This file is a living backlog of ideas that we intentionally postpone until **after** the first working version
(build + tests + runnable CLI) is stabilized.

Guiding rule: **do not optimize early**. We capture ideas here to avoid losing them, then revisit them in later versions
when the pipeline is proven end-to-end.

---

## 1. Compile group sorting once and precompute sort keys in `MemberDescriptor`

### Status
- [ ] Not implemented (captured as a future improvement)
- [ ] Revisit after: tool runs end-to-end + tests are green

### Background
JHarmonizer already has a compiled layer for grouping/classification:
selectors and rule blocks are compiled once into “ready-to-run” predicates, so we can classify `CtTypeMember`s efficiently.

Sorting is still “runtime-heavy”:
- For each group, we rebuild comparator chains based on `SortKey`s.
- We compute sort keys (alpha key, visibility rank, signature key, etc.) repeatedly.
- We introduced extra wrapper DTOs to hold those values, but they are not integrated into the compiled pipeline.

### Problem statement
We want sorting to be as “compiled” and deterministic as grouping:
- No repeated comparator construction per group.
- No repeated computation of sort keys per member.
- Cleaner separation of concerns: *classification prepares data*; *sorting consumes prepared data*.

### Proposed solution
**Move sorting compilation to the same stage as selector compilation.**

1) **Extend `MemberDescriptor`** to hold all computed values needed for sorting:
- `sourceStart` / source position data
- `alphaKey`
- `visibilityRank` (or ranks for ASC/DESC derived from a base rank)
- `signatureKey`
- accessor-related facts used by `keepAccessorsTogether` (property name, accessor kind, return/param type keys, etc.)
- any deterministic tie-breaker values currently derived on-the-fly

2) **Compile a `Comparator<MemberDescriptor>` once per compiled member group**, based on:
- group `SortKey`s (PRESERVE / ALPHA / SOURCE_ORDER / VISIBILITY_ASC / VISIBILITY_DESC / SIGNATURE)
- stable tie-breakers (e.g., sourceStart, signature, deterministic id) to guarantee deterministic output

3) **Reuse `MemberDescriptor` objects throughout the pipeline**:
- The classification step (group selector) consumes the descriptor to decide membership.
- The ordering step sorts descriptors using the already compiled comparator.
- Finally, the renderer uses the stored reference to the original member to reconstruct text.

### Design details

#### A. Descriptor-first pipeline
Instead of passing raw `CtTypeMember` around, we create descriptors once:

- Input: `List<CtTypeMember> typeMembers`
- Map: `CtTypeMember -> MemberDescriptor`
- Group selection: uses compiled predicates on `MemberDescriptor`
- Sorting: uses compiled `Comparator<MemberDescriptor>` from the compiled group
- Output: sorted `CtTypeMember` list via `descriptor.originalMember()`

#### B. Where the comparator lives
Store the comparator on the compiled group (or next to it), for example:
- `CompiledMemberGroupSortingBehavior` (or similar) holds:
  - `List<SortKey> sortKeys`
  - `boolean keepAccessorsTogether`
  - `Comparator<MemberDescriptor> compiledComparator`

This keeps sorting decisions co-located with other compiled group semantics.

#### C. Keep `MemberDescriptor` group-agnostic
`MemberDescriptor` should store *raw facts* and computed keys, not group-specific decisions.

Example:
- Descriptor stores `visibilityRankBase` (or canonical rank).
- Comparator decides ASC vs DESC by comparing ranks in different directions.

#### D. Optional phase-2: generic descriptor
As a follow-up (not part of this item), consider:
- `MemberDescriptor<M>` where `M` is the underlying AST member type (currently `CtTypeMember`).
- Graph units / bundles / representative members could become generic too.

This is **explicitly deferred** until the non-generic version proves beneficial.

### Expected benefits
- Performance: one-time descriptor construction + one-time per-group comparator compilation.
- Cleaner architecture: sorting logic becomes “compiled config” rather than ad-hoc runtime plumbing.
- Consistency: grouping and sorting follow the same “compile once, run many” model.
- Better testability: comparator behavior can be unit-tested using synthetic descriptors.

### Non-goals
- Do not refactor the entire dependency-graph subsystem as part of this item.
- Do not generalize away from Spoon in the first implementation of this idea.
- Do not change output semantics (only reduce repeated work and improve structure).

### Implementation outline (when we revisit this)
- [ ] Identify current “sort key wrapper” DTO(s) and list the computed values required.
- [ ] Extend `MemberDescriptor` to include those values + a reference to the original member.
- [ ] Update the descriptor factory to compute keys once (single pass).
- [ ] Add `Comparator<MemberDescriptor>` compilation to the compiled group stage.
- [ ] Refactor group sorting to sort descriptors using the compiled comparator.
- [ ] Ensure deterministic tie-breakers remain identical to the current behavior.
- [ ] Add unit tests:
  - [ ] comparator correctness for each `SortKey`
  - [ ] stable tie-breaking
  - [ ] `keepAccessorsTogether` scenarios
