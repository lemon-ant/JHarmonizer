# Central Processing Component: `Processor`

## Purpose
`Processor` is the main orchestrator of the system. It performs configuration aggregation first,
then manages the full transformation pipeline:
- Parsing Java source code into an abstract syntax tree (AST)
- Sorting class members according to the specified configuration
- Serializing the sorted AST back to source code
- Formatting the final source code using `PalantirJavaFormat`.

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
