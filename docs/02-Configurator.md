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
