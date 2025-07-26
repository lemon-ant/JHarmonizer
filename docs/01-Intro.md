# JHarmonizer: Java Class Restructuring Tool

## Overview

**JHarmonizer** is a modular tool for restructuring Java source code. Its main purpose is to automatically reorder and
format members of Java classes (fields, constructors, methods, blocks, etc.) to ensure a consistent and readable
structure based on configurable rules.

This tool is especially useful in environments where large teams work on shared codebases and consistency of structure
is important for code reviews, quality checks, or compliance with internal style guides.

## Key Advantages

- **Automated Restructuring**: Eliminates the manual effort of sorting class members.
- **Highly Configurable**: Supports a wide range of rules to control the sorting and formatting logic.
- **Check Mode**: Validates whether source files conform to the desired structure without modifying them.
- **AST-based Manipulation**: Uses abstract syntax trees to ensure syntactic correctness.
- **Extensible Architecture**: Designed for integration with Maven, Gradle, and CI/CD pipelines.

## Main Flow of the Tool

The main flow of JHarmonizer can be summarized as:

1. **Configuration Resolution**: Load user-defined configuration (inline, file, or environment).
2. **Parsing**: Convert Java source into AST using selected parser (e.g., JavaParser, Spoon).
3. **Sorting**: Apply restructuring rules to reorder class members.
4. **Formatting**: Clean up code using an external formatter (e.g., Palantir Java Format).
5. **Diffing (optional)**: In check mode, compare original and modified version for consistency.
6. **Writing (optional)**: In restructure mode, save updated source code to disk or return as string.

Each of these steps is handled by dedicated components, described in detail in the corresponding documentation modules.
