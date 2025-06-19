# What is AST (Abstract Syntax Tree)?

**AST** (Abstract Syntax Tree) is a tree-like data structure used to represent the **syntactic structure** of source code in a hierarchical way.

Each node in the tree corresponds to a **construct** occurring in the code:
- expressions,
- variable declarations,
- method calls,
- class definitions, etc.

## Example

For this Java code:
```java
int x = 2 + 3;
```

The corresponding AST structure would look like:

```
Assignment
├── Variable: x
└── Expression: +
    ├── Literal: 2
    └── Literal: 3
```

## Why it's used

ASTs are used in:
- **code analysis** and **refactoring**,
- **source-to-source transformations** (e.g. sorting class members),
- **linters**, **static analysis**, and **formatters**,
- **code generation** and **compiler backends**.

In `JReStructor`, AST is the core internal representation:
1. Java code is parsed into AST.
2. AST nodes are **reordered** (e.g. class fields, methods).
3. The updated AST is **serialized back to code**.
4. The code is finally **formatted** using Palantir Java Format.

## 📎 Related Tools

- [JavaParser](https://javaparser.org/)
- [Eclipse JDT AST](https://www.eclipse.org/jdt/)
- [ANTLR for Java](https://github.com/antlr/antlr4)
