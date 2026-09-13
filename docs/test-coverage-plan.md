<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# JHarmonizer — test coverage plan (remaining work)

This checklist tracks completed and remaining test coverage.

> Scope: `jharmonizer-core` (config pipeline, Spoon-based sorting, dependency graph, printing, formatting, flows).

## 8) Printing / reconstruction correctness (source slicing)

- [x] **Member body integrity**
  - **Type:** E2E
  - **Targets:** `SpoonSrcPrinter`
  - **Goal:** when a member is moved, its **body text is unchanged** (except group separator insertion around
    boundaries).
  - **Must assert:** stable “sentinel” markers inside members stay exactly the same.
  - **Covered by:** `SrcPrinterE2ETest` uses full-output fixtures and whitespace variants through the shared E2E runner,
    with formatting disabled.

- [x] **Boundary stitching correctness**
  - **Type:** E2E
  - **Targets:** `SpoonSrcPrinter`, `SrcPrinterOutput`, `SrcCodeUtils`
  - **Goal:** no missing/extra braces, semicolons, or accidental merges of two members.
  - **Must assert:** output is syntactically valid and compiles.
  - **Covered by:** `SrcPrinterE2ETest` checks exact boundaries with formatting disabled, combined separator requests,
    LF/CRLF/CR endings, space/tab indentation, opt-out fragments, and repeated processing. Output compiles with JDK 21;
    see [printer coverage](printer-coverage.md).

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
