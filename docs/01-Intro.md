<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# JHarmonizer: Java Class Reordering Tool

## Overview

**JHarmonizer** is a modular tool for reordering Java source code. Its main purpose is to automatically reorder and
format members of Java classes (fields, constructors, methods, blocks, etc.) to ensure a consistent and readable
structure based on configurable rules.

This tool is especially useful in environments where large teams work on shared codebases and consistency of structure
is important for code reviews, quality checks, or compliance with internal style guides.

## Key Advantages

- **Automated Reordering**: Eliminates the manual effort of sorting class members.
- **Highly Configurable**: Supports a wide range of rules to control the sorting and formatting logic.
- **Check Mode**: Validates whether source files conform to the desired structure without modifying them.
- **AST-based Manipulation**: Uses abstract syntax trees (Spoon) to ensure syntactic correctness.
- **Multiple front-ends**: Ships with a CLI fat JAR (`jharmonizer-cli`) and a Maven plugin
  (`jharmonizer-maven-plugin`); a Gradle plugin is not currently shipped.

## Main Flow of the Tool

The main flow of JHarmonizer can be summarized as:

1. **Configuration Resolution**: Load the embedded default configuration and optionally
   merge a user-supplied YAML file and CLI/Maven parameter overrides on top.
2. **Parsing**: Convert each Java source into an AST using **Spoon** (`SpoonParser`).
3. **Sorting**: Apply the configured ordering rules, honouring the declaration-order
   dependency graph and opt-out directives.
4. **Serialization**: Render the reordered Spoon AST back to Java source text via
   the customized Spoon printer (`SpoonCustomSrcPrinter`), preserving comments,
   annotations, and any source ranges marked by `@jharmonizer:fully-off`.
5. **Formatting**: Run the **Palantir** java-format pass and apply blank-line and
   import-fixing rules.
6. **Diffing (optional)**: In check mode, compare the rewritten text to the original
   to determine whether the file conforms.
7. **Writing (optional)**: In `reorder` mode, write the updated source back to disk
   (with an optional `.bak` backup).

Each of these steps is handled by dedicated components, described in detail in the corresponding documentation modules.
