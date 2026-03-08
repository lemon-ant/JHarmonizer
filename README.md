# JHarmonizer

JHarmonizer is a Java source harmonization tool that keeps class member layout deterministic and readable.
It parses Java source, resolves grouping/sorting rules, applies dependency-safe reordering, and formats output.

## Current status

- Core pipeline is available: parse -> classify -> sort -> print -> format.
- Declaration-order dependency graph is implemented for direct initializer/read-write scenarios and accessor bundling.
- Advanced inter-procedural dependency tracing for field initializers through called methods is **not implemented yet**.

## Roadmap (next versions)

- Add opt-out suppression markers per file/type.
- Compile sorting behavior once per group and precompute reusable sort keys in member descriptors.
- Add selector matching by type (field type / method return type).
- Add explicit enum constant ordering strategies.
- Add support for corner-cases with explicit declaring-type instance forward references during class initialization (currently parked as known failing E2E scenario).
- Add inter-procedural initializer dependency analysis:
  - if a field default expression calls a method, inspect method body reads/writes;
  - recursively follow nested method calls inside the same declaring type;
  - build declaration dependencies based on transitively accessed fields;
  - handle recursion/cycles safely and conservatively.

Details and implementation notes are tracked in [docs/TODO.md](docs/TODO.md).

## Build

Default (local / IDE / CI that relies on project config):

```bash
mvn -B -ntp verify
```

This uses `.mvn/maven.config`, which points to `.mvn/settings.xml`.

Codex/runtime mode (proxy-enabled settings override via environment variable):

```bash
export MAVEN_ARGS="--settings .mvn/settings-codex.xml"
mvn -B -ntp verify
```

Recommended Codex env var value:

```bash
MAVEN_ARGS=--settings\ .mvn/settings-codex.xml
```

So local environments keep using `.mvn/settings.xml`, while Codex overrides settings through `MAVEN_ARGS` only.
