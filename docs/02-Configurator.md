# Configurator

## Purpose

To construct a validated, immutable configuration model (`Configuration`) by:

- loading internal defaults from resources,
- overlaying with external configuration sources (IDE (IDEA, possibly Eclipse), configuration file, passed model from external wrapper),
- resolving priority and applying overrides in sequence.

## Configuration Assembly Flow

1. Load default configuration from embedded resource file  
   → `Configuration defaultConfig`

2. Optionally overlay:
   - IDE configuration (optional) - it must be IDEA and possibly Eclipse
   - Custom file configuration (optional)
   - External override (optional)

3. Each overlay produces a merged intermediate:  
   `defaultConfig → mergedConfig1 → mergedConfig2 → ... → finalConfig`

## Key Components (suggested interfaces only)

| Component                 | Responsibility                                                                   |
|---------------------------|----------------------------------------------------------------------------------|
| `DefaultConfigLoader`     | Loads embedded default configuration from `resources/`.                          |
| `IDEConfigParser`         | Parses IDE-specific configuration formats (e.g. `.editorconfig`, `.idea/*.xml`). |
| `ProjectFileConfigParser` | Reads XML configuration from project sources.                                    |
| `Configurator`            | Coordinates the full assembly process; returns validated `Configuration`.        |

These additional configuration parsers can implement a unified interface `OverridingConfigurationProvider` to be 
processed as a sequence in a unified stream/cycle algorithm. 

## Proposed Core Types

All code snippets given only as illustrations of the idea

### 1. `OverridingConfiguration`

A nullable, non-validated DTO representing raw configuration input.

Used as intermediary for loading from:

- default resource file
- IDE settings (JetBrains, Eclipse)
- project configuration file (XML/YAML)
- external override models (e.g. plugin input)

```java
@Data
public class OverridingConfiguration {
    private List<String> memberOrder;
    private List<String> accessLevelOrder;
    private Integer maxLineLength;
    private Boolean reorderImports;
}
```

## 2. `Configuration`

Final, validated, immutable model for internal use.

```java
@Value
@Builder
public class Configuration {
    List<String> memberOrder;
    List<String> accessLevelOrder;
    int maxLineLength;
    boolean reorderImports;
}
```

## Proposed method contracts

All code snippets given only as illustrations of the idea

### Resolve Signature

```java
public interface Configurator {
    Configuration resolve(
        List<OverridingConfiguration> externalConfigs,
        List<Path> configFiles
    );
}
```

### Merge Signature

```java
public interface Configurator {
   Configuration merge(Configuration baseConfig, OverridingConfiguration overridingConfiguration);
}
```

## Notes

- `OverridingConfiguration` can be reused across all stages as the universal nullable intermediate.
- Merging logic should handle null fields safely, always preserving previous values unless explicitly overridden.
- Multiple `merge()` calls are expected in sequence.
- Final `Configuration` must be fully initialized and validated (non-null + correct values).

## Root member groups merge semantics

When an external model overrides `type-members-ordering`, JHarmonizer does not replace the whole default root-group list anymore.
The merge is performed only for the **root member groups**, without recursive subgroup merging:

1. If an external root group has a `name` that exactly matches a default root-group name, the default root group is removed.
2. The external root group is inserted into the **same position** where the matched default group originally was.
3. If an external root group name does not exist in the default configuration, that external group is treated as new.
4. All new external root groups are inserted **before** the default root groups, preserving their order from the external model.
5. Nested `groups:` blocks are not merged by name. Once a root group matches, its whole subtree from the external model replaces the default subtree as-is.

This allows users to redefine only selected default root groups while keeping all untouched default groups and their original ordering.

## Optional Configuration Sources Control

For advanced usage, the configurator supports **optional control over which sources to include** when resolving the final configuration.

By default, all sources are enabled:
- IDE configuration (e.g. IntelliJ, Eclipse)
- Project-level configuration files (XML/YAML)
- External override inputs (e.g. plugin parameters)

However, users can pass boolean flags (e.g. `disableIDEA`, `disableFileConfig`) to suppress specific sources.

Additionally, for full flexibility, it is proposed that the configurator exposes a method accepting a **custom sequence of configuration parsers**, each implementing a common interface (e.g. `RawConfigProvider`).

This allows advanced consumers (e.g. plugins, test suites) to **control exactly which parsers participate** in the configuration resolution process.

```java
public interface OverridingConfigurationProvider {
    OverridingConfiguration load();
}
```

Suggested method for customized merging:

```java
public Configuration resolve(List<OverridingConfigurationProvider> customProviders);
```

This approach makes it possible to:
- Test specific parsers in isolation
- Apply only trusted configuration layers (e.g. CI pipelines)
- Extend or replace default parsing logic
