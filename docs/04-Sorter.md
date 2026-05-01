<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Sorter

## Purpose

The sorter applies the compiled JHarmonizer configuration to Spoon AST models so top-level types and type members appear in deterministic, readable order without violating modeled declaration-order dependencies.

## What gets sorted

The default configuration covers:

- top-level classes, records, interfaces, enums, and annotations;
- record components;
- enum constants;
- fields;
- static and instance initializer blocks;
- constructors;
- methods;
- nested classes, records, interfaces, enums, and annotations.

## Configuration model

Sorting is driven by `type-members-ordering` root groups. A root group first selects which types it applies to, then nested member groups classify members by selectors such as kind, visibility, modifier, annotation, name, or regular expression. Group options can define separators, accessor clustering, forward-reference strictness, and ordering rules.

Top-level type sorting is configured separately under `top-level-types-ordering`.

## Ordering rules

Current YAML ordering rules are:

- `preserve` — source order;
- `alpha` — computed alphabetical/signature key;
- `visibility-desc` — public → protected → package-private → private;
- `visibility-asc` — private → package-private → protected → public.

## Accessor clustering

When `keepAccessorsTogether` is enabled for a group subtree, JavaBean-style accessors are clustered by property. The comparator uses cluster representative keys before falling back to each member's own key, so related getters/setters stay together while the group remains deterministic.

## Dependency safety

After the preferred visual order is computed, declaration-order dependency handling protects modeled provider-before-dependent relationships. The dependency model covers direct initializer-style references and related forward-reference cases represented by the current provider chain. If a preferred order conflicts with these constraints, dependency safety wins.

Cycle handling and strict/relaxed forward-reference behavior are part of the dependency-aware ordering pipeline and are exercised by the end-to-end fixture tests under `core/src/test/resources/test-cases/core/e2e/reorder/**`.
