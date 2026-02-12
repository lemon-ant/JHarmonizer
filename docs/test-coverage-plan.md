# JHarmonizer — test coverage plan (remaining work)

This document is a **checklist of test work that is still missing** in the current repo version.
Use it as a contract/roadmap: we will implement tests **one item at a time** and tick them off.

> Scope: `jharmonizer-core` (config pipeline, Spoon-based sorting, dependency graph, printing, formatting, flows).

---

## What is already covered (context, no action)
Already present tests give us confidence in:
- Vendor YAML loading and model deserialization (including snapshots),
- Vendor → Unified conversion (including snapshot),
- Unified DTO invariants and flag/mask encoding,
- Files discovery/backup/write via globs,
- Palantir formatter integration (basic cases),
- Spoon parse/serialize smoke + Java 21 syntax smoke,
- A thin `SourceProcessor` smoke on temp FS.

Everything below is **still missing** and is the focus of this plan.

---

## 6) Ordering inside a group (sort keys + tie-breakers)

- [ ] **SortKey.PRESERVE**
  - **Type:** component
  - **Targets:** `GroupMembersOrderer`, `ComparatorUtils`
  - **Goal:** exact original source order is kept.
  - **Must assert:** stable ordering even with equal keys.

- [ ] **SortKey.ALPHA**
  - **Type:** component
  - **Targets:** `ComparatorUtils`, `SpoonTypeMemberUtils`
  - **Goal:** alphabetical ordering is deterministic (and locale-independent).
  - **Must assert:** tie-breakers (sourceStart/signature) are applied consistently.

- [ ] **SortKey.SOURCE_ORDER**
  - **Type:** component
  - **Targets:** `RelocationDetector`, `SpoonTypeMemberUtils`
  - **Goal:** ordering respects original positions as the primary key.
  - **Must assert:** correct handling after reordering (stable referencing to original positions).

- [ ] **SortKey.VISIBILITY_ASC / VISIBILITY_DESC**
  - **Type:** component
  - **Targets:** `ComparatorUtils`
  - **Goal:** visibility rank mapping is correct and both directions are correct.
  - **Must assert:** exact rank ordering for public/protected/package-private/private.

- [ ] **SortKey.SIGNATURE**
  - **Type:** component
  - **Targets:** `ComparatorUtils`, signature builder used in sorter
  - **Goal:** signature ordering is stable and does not depend on environment-specific Spoon output.
  - **Must assert:** deterministic order for overload sets and generics.

- [ ] **keepAccessorsTogether effect on final order**
  - **Type:** component
  - **Targets:** `GroupMembersOrderer`
  - **Goal:** bundling is respected without violating declaration dependencies.
  - **Must assert:** dependencies win over bundling where required.

---

## 7) Group separators and headers in output

- [ ] **Boundary metadata placement**
  - **Type:** component
  - **Targets:** `GroupBoundaryMarker`, `SpoonSourcePrinterUtils`
  - **Goal:** metadata is written only to the first member of each non-empty group.
  - **Must assert:** empty groups do not emit separators.

- [ ] **SeparatorDirective behavior**
  - **Type:** component
  - **Targets:** printer integration of `separatorDirective`
  - **Goal:** `HEADER` prints group name header; `NEW_LINE` prints a blank line; `NONE` prints nothing.
  - **Must assert:** exact emitted text for each directive.

---

## 8) Printing / reconstruction correctness (source slicing)

- [ ] **Member body integrity**
  - **Type:** component
  - **Targets:** `SpoonCustomSourcePrinter`
  - **Goal:** when a member is moved, its **body text is unchanged** (except group separator insertion around boundaries).
  - **Must assert:** stable “sentinel” markers inside members stay exactly the same.

- [ ] **Boundary stitching correctness**
  - **Type:** component
  - **Targets:** `SpoonCustomSourcePrinter`, `SpoonSourcePrinterUtils`
  - **Goal:** no missing/extra braces, semicolons, or accidental merges of two members.
  - **Must assert:** output is syntactically valid and compiles.

- [ ] **Package/import/header handling**
  - **Type:** component
  - **Targets:** printer + formatter integration
  - **Goal:** output has correct `package`, imports and class header, and formatter does not break it.
  - **Must assert:** import cleanup works as expected when enabled.

---

## 9) Records and enums — explicit contracts (current product decisions)

- [ ] **Record: implicit fields processing is disabled (current behavior)**
  - **Type:** component/E2E
  - **Targets:** sorter + printer
  - **Goal:** record implicit fields are not processed/reordered; record remains valid.
  - **Must assert:** methods/ctors/nested types ordering still applies and output compiles.

- [ ] **Enum constants preserve order**
  - **Type:** component/E2E
  - **Targets:** grouping + ordering
  - **Goal:** enum constants remain in source order (unless explicitly configured otherwise).
  - **Must assert:** methods and nested types obey configured rules.

- [ ] **Unconfigured/unknown modifiers behavior**
  - **Type:** unit/component
  - **Targets:** config compiler + descriptor factory
  - **Goal:** modifiers not referenced in config do not break classification/ordering.
  - **Must assert:** safe default handling.

---

## 10) Flow-level behavior beyond smoke (status + exceptions)

- [ ] **CHECK_FAIL_FAST status contract**
  - **Type:** integration
  - **Targets:** `CheckFailFastFlow`
  - **Goal:** first violation stops processing with correct exception type/status.
  - **Must assert:** no additional files are processed after first failure.

- [ ] **CHECK_ALL aggregation contract**
  - **Type:** integration
  - **Targets:** `CheckAllFlow`, `SourceProcessingStats`
  - **Goal:** processes all files, aggregates counts/times/statuses deterministically.
  - **Must assert:** error cases are reported but do not prevent other files.

- [ ] **RESTRUCTURE file-write + backup contract**
  - **Type:** integration/E2E
  - **Targets:** `RestructureFlow`, `SourceFilesHandler`
  - **Goal:** file rewrite and backup naming/placement follow configuration.
  - **Must assert:** backups created only when enabled.

- [ ] **FlowDebugStageRecorder contract**
  - **Type:** integration
  - **Targets:** `FlowDebugStageRecorder`
  - **Goal:** stage dumps are created as configured and do not affect outputs.
  - **Must assert:** content and naming are stable.

---

## 11) Full E2E fixtures + compilation before/after (release-quality gate)

> This is the “professional guarantee” layer.

- [ ] **Fixture framework in tests**
  - **Type:** test infrastructure
  - **Goal:** standard fixture layout and helpers:
    - `fixtures/<scenario>/input/**`
    - `fixtures/<scenario>/expected/**`
    - `fixtures/<scenario>/config.yml`
  - **Must define:** normalization rules (EOL, trailing spaces, file encoding).

- [ ] **Compile-before and compile-after helper**
  - **Type:** test infrastructure
  - **Goal:** compile Java sources with `--release 21` for both input and output.
  - **Must assert:** output always compiles for “valid input” scenarios.

- [ ] **E2E scenario set (initial wave)**
  - **Type:** E2E
  - **Scenarios to implement:**
    1. Fields + initializers + methods + nested types (baseline ordering)
    2. Static vs instance init-blocks (the “static modifier, not kind” contract)
    3. Field initializer dependency chain (A->B->C)
    4. Cycle handling (SCC bundling)
    5. keepAccessorsTogether (get/is/has/set)
    6. Enum constants preserve + methods ordering
    7. Record (implicit fields disabled) + methods ordering
    8. Separators (HEADER/NEW_LINE/NONE) visible in output
  - **Must assert:** exact expected output + compilation after processing.

---

## Tracking
Suggested workflow for ticking items:
1. Implement the smallest unit/component test that fixes the contract.
2. Add one E2E scenario only when the underlying layers are stable.
3. Keep E2E fixtures minimal but “syntactically dense” (sentinel comments inside members).

