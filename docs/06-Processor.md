# Central Processing Component: `Processor`

## Purpose
`Processor` is the main orchestrator of the system. It performs configuration aggregation first,
then manages the full transformation pipeline:
- Parsing Java source code into an abstract syntax tree (AST)
- Sorting class members according to the specified configuration
- Serializing the sorted AST back to source code
- Formatting the final source code using `PalantirJavaFormat`.

## Dual Flow Overview

The Processor supports two distinct execution flows:

### 1. Restructure Flow
- Full processing pipeline: config → parse → sort → serialize → format
- Produces a new source code
- Optionally writes to file(s)
- Can return stats (count, size, duration)

### 2. Check Flow
- Same pipeline as above, but only in memory
- Compares input and output
- Throws if restructuring would alter content
- Used for CI/linting


## Public API Methods

### 1. Process a single Java source code string
```java
String restructure(String inputJavaCode, OverridingConfiguration configuration, Path configFile, ConfigParserFlags flags);
```
- **Input**:
  - Java source code string
  - Configuration input (`OverridingConfiguration`)
  - Flags indicating which config parsers are enabled/disabled
- **Output**: Transformed and formatted Java code string

### 2. Process a single Java file
```java
FileProcessingStatisitic restructure(Path inputFilePath, OverridingConfiguration configuration, Path configFile, ConfigParserFlags flags);
```
- Reads file content, processes it, and overwrites or replaces the original file
- **Output**: Processing statistic: processing time, size before and after transformation, error count, etc.

### 3. Process a directory of Java files (recursively, in parallel)
```java
ProcessingReport restructure(Path inputDirectory, OverridingConfiguration configuration, Path configFile, ConfigParserFlags flags);
```
- Recursively processes `.java` files in the directory
- Executes processing in parallel threads
- **Output**: `ProcessingReport` containing:
  - Number of processed files
  - Total and average processing time
  - Min/max file size
  - Error count
  - etc.

## Internal Processing Flow

1. **Configure**: All entry points first invoke `Configurator` to collect and merge all configuration sources into a final `Configuration`.
2. **Parse**: Java source is parsed into an AST via a chosen parser (e.g., `JavaParser`, `Spoon`).
3. **Sort**: Members of the AST are reordered using `ASTSorter` based on configuration.
4. **Serialize**: AST is converted back to Java source code.
5. **Format**: Final code is formatted using `formatSourceAndFixImports()` with the appropriate style (Palantir/Google/AOSP).

## Extensibility and Flexibility

- **Multi-source configuration support**: YAML, IDE settings, inline configs.
- **Parser control flags**: selectively enable/disable configuration sources.
- **Contextual outputs**: method output varies depending on target (string, file, directory).
- **Metrics & logging hooks**: optionally integrated for performance tracking and diagnostics.

## 🔍 Check Mode (Validation Without Rewrite)

In addition to full restructuring, the Processor also provides a **`check` mode**.  
This flow performs the same pipeline (configuration → parsing → sorting → serialization → formatting) but **only in memory** and does **not write any changes**.  
It compares the original input with the result and throws an exception if restructuring would make changes.

### Purpose:
Used in CI or quality gates to validate whether code is already correctly structured.

### Public Methods:

All configuration parameters were omitted for simplicity.

- `void check(Path directory)`
  - Recursively checks all `.java` files in a directory.
  - If any file differs from its restructured version, throws `CodeNotRestructuredException`.

- `void check(Path file)`
  - Checks a single file.

- `void check(String javaSource)`
  - Checks a raw Java source string.

### Exception Behavior:

If restructuring changes the code, a `CodeNotRestructuredException` is thrown.  
The exception should contain:
- Path or origin description
- Unified diff for diagnostics


## Additional Note: Compilation Validation Step

In certain cases, if the selected AST parser fails to clearly identify invalid Java source files (e.g., files with 
syntactical or structural corruption), it may be necessary to integrate a lightweight and embeddable Java compiler into
the processing pipeline. 

The idea is to pre-validate each file by attempting to compile it before parsing and restructuring. This compilation
phase can serve as a safeguard to ensure that the file is valid Java code. If the compiler fails with a meaningful 
error message, it can prevent the restructuring pipeline from executing on a broken file and help log or report the 
cause of failure.

This step may be performed in parallel or as a pre-processing phase before invoking the AST parser. The need for this 
step will depend on the behavior of the selected parser and should be evaluated after initial POC validation.

## Backup Handling Before File Overwrite

To prevent accidental data loss when overwriting files during restructuring, especially in environments where no version control is used, implement an optional backup mechanism.

- **Condition**: A backup file should be created **only if** the output differs from the original input (i.e. the file is modified).
- **Backup naming convention**: Append a `.bak` or `.backup` extension to the original filename (e.g. `MyClass.java.bak`).
- **Activation**: This feature should be configurable via `Configuration`, e.g. a flag like `createFileBackup = true`.
- **Rationale**: Enables safe recovery if something goes wrong during restructuring, or if a user prefers to manually inspect changes.
