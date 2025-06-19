# Java AST Parser

## Purpose

In the context of the JReStructor project, we aim to evaluate and select a reliable and flexible Java parser 
that transforms raw Java source code into an object-oriented model (AST - Abstract Syntax Tree). 
This is a critical step for performing member reordering and clean reserialization back into Java source. 
Post-sorting, raw code might lose its original formatting and readability. Thus, to preserve correctness and maintain 
format quality, we must rely on a robust parser + formatter pair.

The AST parser must serve as the foundation for:
- breaking code into structured elements,
- performing sorting based on rules and configurations,
- and reserializing it reliably, preserving annotations, comments, formatting consistency, and correct Java syntax.

## What is AST (Abstract Syntax Tree)?

**AST** (Abstract Syntax Tree) is a tree-like data structure used to represent the **syntactic structure** of source
code in a hierarchical way.

Each node in the tree corresponds to a **construct** occurring in the code:
- expressions,
- variable declarations,
- method calls,
- class definitions, etc.

### Example

For this Java code:
```java
int x = 2 + 3;
```

The corresponding AST structure would look like:

```
Assignment
+- Variable: x
\- Expression: +
    +- Literal: 2
    \- Literal: 3
```

## Where It Fits in the Pipeline

```
Raw Java code
    ↓
AST Parser (e.g., JavaParser or Spoon)
    ↓
Object Model (Class → Fields, Methods, Inner Types, etc.)
    ↓
Sorting and processing
    ↓
Back to source (reserialization)
    ↓
Formatter (Palantir Java Format)
    ↓
Final formatted Java code
```

## Candidate Libraries

### 1. [JavaParser](https://javaparser.org/)
- Maintained actively, supports Java 1-21
- Simple API for parsing, visiting, and manipulating AST
- Supports pretty-printing with formatting preservation
- Comments and annotations handled explicitly
- Can parse broken/incomplete Java code (with exceptions)

### 2. [Spoon](https://spoon.gforge.inria.fr/)
- Academic-grade deep Java code analysis and transformation framework
- More powerful than JavaParser but more complex
- AST with rich semantic information
- Ideal for deep refactoring, not just basic sorting
- Also supports comments and annotations

### 3. Eclipse JDT AST
- The AST used by the Eclipse IDE
- Heavy, low-level API
- Requires Eclipse infrastructure
- Not ideal for lightweight standalone tools

### 4. ANTLR with Java grammar
- Manual integration with grammar definitions
- Error-prone and requires high effort
- Not considered suitable for our primary flow

## Parser Selection Criteria (POC Plan)

We must test:
1. **Valid Java Source**
    - Parse a valid `.java` file
    - Check resulting AST structure
    - Evaluate reserialization output

2. **Broken Java Source**
    - Test malformed files
    - Determine how gracefully parser fails
    - Expected: structured error or partial AST

3. **Comments Preservation**
    - Block, inline, and Javadoc comments
    - Must survive roundtrip (parse + output)

4. **Annotations**
    - On fields, methods, classes
    - Preservation + *ability to sort annotations*

5. **Inner/Nested Classes**
    - Must retain proper nesting in AST and output
    - Especially test deep nesting

6. **Field Dependency Sorting**
    - Determine order based on other field usage
    - Does AST contain information about field dependencies?

7. **Reserialization Quality**
    - Check fidelity of output
    - Formatting errors or losses?

8. **Java Version Compatibility**
    - Ensure Java 21 features are parsed correctly

## Additional Notes

- We may wrap the parser of choice in a lightweight adapter component to abstract its API and expose a simplified JReStructor-specific interface.
- This component will serve as the core input/output layer for sorting and transformation logic.

## Summary

The selected parser must:
- Be easy to use and integrate
- Provide good support for annotations and comments
- Work with Java 21 syntax
- Tolerate broken code inputs gracefully
- Output clean, faithful source via serialization

### Primary candidates:
- JavaParser (Preferred starting point)
- Spoon (Alternative with deeper insight)

### Secondary fallback:
- Eclipse JDT AST (fallback)
- ANTLR (fallback)

If needed, fallback parser integration may be considered for future iterations depending on POC results and resource availability.

## Related Tools

- [JavaParser](https://javaparser.org/)
- [Eclipse JDT AST](https://www.eclipse.org/jdt/)
- [ANTLR for Java](https://github.com/antlr/antlr4)
