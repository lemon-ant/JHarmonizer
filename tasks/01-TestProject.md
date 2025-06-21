# Task 01 – Locate or Assemble a Comprehensive Java Test Project

## Objective

We need a fully compilable Java project that includes a wide variety of Java constructs (up to Java 21) to serve as:

- A test input for our parser, sorter, and formatter
- A baseline for comparing parsers (e.g., JavaParser vs Spoon)
- A regression test base for CI pipelines
- A demonstrative input for product showcases

## Primary Strategy: Reuse First

Before manually assembling a synthetic project, **we must first try to locate an existing test corpus** from:

- [Google Java Format](https://github.com/google/google-java-format)
- [Palantir Java Format](https://github.com/palantir/palantir-java-format)
- OpenJDK compiler test suites
- Eclipse JDT or other compiler tools

These are likely to already include many edge-case Java constructs.

## Fallback Plan: Manual Assembly

If no sufficient resource is found, we must create a synthetic Java project with full coverage of Java syntax and structure.

## Mandatory Constructs to Include

### Top-Level Structures

- package-info.java
- module-info.java
- Classes, Interfaces, Enums, Annotations, Records

### Modifiers and Nesting

- sealed, non-sealed, final classes with permits
- Deeply nested classes and interfaces

### Blocks and Methods

- Static and instance initializers
- Javadoc comments
- Inline and block comments: //, /*...*/, /**...*/
- Text blocks (triple-quoted)
- Methods with multiple modifiers and annotations
- Local classes inside methods
- Method references
- Constructors using super() and this()

### Fields

- With interdependent initializers
- Annotated at various levels
- volatile and transient modifiers
- Complex initializers
- Array initializations (1D and multidimensional)

### Functional Features

- Lambdas
- Stream pipelines: map(), filter(), collect()
- Optionals

### Control Flow and Language Features

- switch blocks and switch expressions (Java 14+)
- try-with-resources, multi-catch
- yield
- var keyword
- Records (Java 16+)
- Sealed classes/interfaces (Java 17+)

### Miscellaneous

- Annotations at package level
- Custom exceptions
- Anonymous classes
- Generic classes and methods
- import and import static
- Dangling comments not attached to code

## Requirements

- Must compile successfully on **JDK 21**
- Does not need to execute, but must be syntactically and semantically valid
- Should be structured as a multi-file Java project, not a single monolithic file
