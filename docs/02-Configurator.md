# Configurator

## Purpose

To construct a validated, immutable configuration model (`Config`) by:

- loading internal defaults from resources,
- overlaying with external config sources (IDE (IDEA, possibly Eclipse), configuration file, passed model from external wrapper),
- resolving priority and applying overrides in sequence.

## Configuration Assembly Flow

1. Load default config from embedded resource file  
   → `Config defaultConfig`

2. Optionally overlay:
   - IDE config (optional) - it must be IDEA and possibly Eclipse
   - Custom file config (optional)
   - External override (optional)

3. Each overlay produces a merged intermediate:  
   `defaultConfig → mergedConfig1 → mergedConfig2 → ... → finalConfig`

## Key Components (suggested interfaces only)

| Component                 | Responsibility                                                                   |
|---------------------------|----------------------------------------------------------------------------------|
| `DefaultConfigLoader`     | Loads embedded default config from `resources/`.                                 |
| `IDEConfigParser`         | Parses IDE-specific configuration formats (e.g. `.editorconfig`, `.idea/*.xml`). |
| `ProjectFileConfigParser` | Reads XML config from project sources.                                           |
| `Configurator` | Coordinates the full assembly process; returns validated `Config`.    |

## Proposed Core Types

All code snippets given only as illustrations of the idea

### 1. `OverridingConfig`

A nullable, non-validated DTO representing raw config input.

Used as intermediary for loading from:

- default resource file
- IDE settings (JetBrains, Eclipse)
- project config file (XML/YAML)
- external override models (e.g. plugin input)

```java
@Data
public class OverridingConfig {
    private List<String> memberOrder;
    private List<String> accessLevelOrder;
    private Integer maxLineLength;
    private Boolean reorderImports;
}
```

## 2. `Config`

Final, validated, immutable model for internal use.

```java
@Value
@Builder
public class Config {
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
    Config resolve(
        List<OverridingConfig> externalConfigs,
        List<Path> configFiles
    );
}
```

### Merge Signature

```java
public interface Configurator {
    RawConfig merge(Config baseConfig, OverridingConfig overridingConfig);
}
```

## Notes

- `OverridingConfig` can be reused across all stages as the universal nullable intermediate.
- Merging logic should handle null fields safely, always preserving previous values unless explicitly overridden.
- Multiple `merge()` calls are expected in sequence.
- Final `Config` must be fully initialized and validated (non-null + correct values).
