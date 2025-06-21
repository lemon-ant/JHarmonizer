# Sorter

## Purpose

This document describes the design and responsibilities of the member sorting component in the JReStructor tool.
The purpose of the sorter is to provide consistent, configurable ordering of all class members in a Java source file,
to ensure readability, maintainability, and stability of diffs.

## What Gets Sorted

The sorter is responsible for ordering the following elements within a Java class:

- Fields
- Initializer blocks (static and instance)
- Constructors
- Methods (static and instance)
- Inner classes and interfaces

## Input

The sorter operates on:

- An **AST model** of a parsed Java class (produced by a separate parser step).
- A **Configuration** object containing:
  - Expected member ordering (e.g. fields → constructors → methods)
  - Visibility ordering (e.g. public → protected → package-private → private)
  - Sorting rules within categories (e.g. alphabetical)
  - Options for nested class processing
  - Special cases: getter/setter pairs, constructors, initialization blocks

## Algorithm Overview

1. **Group Members by Type**:
   All members are classified (fields, methods, etc.) and grouped accordingly.

2. **Recursive Processing of Inner Classes**:
   The sorter recursively applies itself to inner classes using the same configuration.

3. **Sort All Categories Except Fields**:
   Standard sorting is applied:
   - First by visibility
   - Then by alphabetic name (if configured)

4. **Special Handling for Fields**:
   Fields may depend on each other:
   ```java
   int b = 42;
   int a = b + 1;
   ```
   In this case, `b` must appear **before** `a`, regardless of alphabetical or visibility preferences.

   The algorithm must:
   - Build a **dependency graph** of fields
   - Attempt to apply desired order **without violating dependency constraints**
   - If conflicts arise, prioritize correctness and preserve original order as fallback

5. **Output**:
   - An updated AST with members sorted according to the resolved order.

## Design Considerations

- **Field Dependencies** are the most complex challenge.
- The algorithm must be **deterministic** and repeatable.

## Testing Strategy for Fields Resorting

Even before final parser selection, the sorting logic can be prototyped using in-memory mock models that simulate 
class members and their relationships. This allows testing of:

- Sorting correctness
- Dependency resolution
- Corner cases like:
  - Circular dependencies
  - Annotated fields/methods
  - Static and non-static member interleaving

## Requirements

- Recursively support nested classes
- Fully configurable via `Configuration`
- Operate on an AST model (from JavaParser or Spoon, etc.)
- Compatible with Java 21 constructs
