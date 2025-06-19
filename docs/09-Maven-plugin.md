# Maven Plugin: `jrestructor-maven-plugin`

## Purpose

This Maven plugin is designed to integrate the **JReStructor** utility into Java projects as a build phase tool. 
It enables automated restructuring or structure validation of Java source files directly from Maven, with flexible 
configuration and execution modes.

## Features

- Run structure **reformatting** or **check-only validation** as part of the Maven lifecycle.
- Configurable to operate on `src/main/java` by default, with an option to include `src/test/java`.
- Three validation severity levels for `check` mode.
- Executes before source code generation phase to prevent working on already generated code.
- Accepts inline configuration via `<configuration>` section in `pom.xml`.
- Supports backup of modified files if desired.

## Plugin Goals

- `jrestructor:check` — validates whether files are already properly structured.
- `jrestructor:restructure` — restructures Java source files according to the defined sorting and formatting logic.

## Configuration Options

| Parameter            | Type      | Description                                                        |
|----------------------|-----------|--------------------------------------------------------------------|
| `mode`               | `String`  | Either `check` or `restructure`.                                   |
| `includeTestSources` | `boolean` | Whether to include `src/test/java` in addition to `src/main/java`. |
| `severityLevel`      | `String`  | `fail-fast`, `collect-and-fail`, or `warn-only` for check mode.    |
| `configFiles`        | `List`    | Optional paths to config files to override defaults.               |
| `overrideConfigs`    | `List`    | Optional list of inline configuration overrides.                   |
| `parserFlags`        | `List`    | Optional parser customization flags.                               |
| `enableBackup`       | `boolean` | Whether to create backups before overwriting any files.            |

## Maven Phase

By default, the plugin is configured to execute before `generate-sources`, ensuring that only manually written Java 
files are processed.

## Sample Usage

```xml
<plugin>
  <groupId>com.example</groupId>
  <artifactId>jrestructor-maven-plugin</artifactId>
  <version>0.1.0</version>
  <executions>
    <execution>
      <goals>
        <goal>check</goal>
      </goals>
      <phase>generate-sources</phase>
    </execution>
  </executions>
  <configuration>
    <mode>check</mode>
    <includeTestSources>true</includeTestSources>
    <severityLevel>collect-and-fail</severityLevel>
    <enableBackup>true</enableBackup>
  </configuration>
</plugin>
```

## Testing Strategy

The plugin will use the **Maven Plugin Testing Framework** (e.g. `org.apache.maven.plugin.testing`) or alternatives 
such as **Invoker Plugin** for full integration testing.

Tests will:

- Ensure valid detection and behavior under all severity levels.
- Verify file changes and backup creation.
- Confirm plugin integration works across multi-module projects and test configurations.

## Notes

This document is a **draft specification**. Some configuration keys and plugin behaviors may be refined during implementation.
