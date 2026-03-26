# JHarmonizer — test coverage plan (remaining work)

This document is a **checklist of test work that is still missing** in the current repo version.
Use it as a contract/roadmap: we will implement tests **one item at a time** and tick them off.

> Scope: `jharmonizer-core` (config pipeline, Spoon-based sorting, dependency graph, printing, formatting, flows).

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
