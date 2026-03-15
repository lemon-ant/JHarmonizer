# JHarmonizer

JHarmonizer is a Java source harmonization tool that keeps class member layout deterministic and readable.
It parses Java source, resolves grouping/sorting rules, applies dependency-safe reordering, and formats output.

## Current status

- Core pipeline is available: parse -> classify -> sort -> print -> format.
- Declaration-order dependency graph is implemented for direct initializer/read-write scenarios and accessor bundling.
- Comment-based opt-out directives are supported for file scope and type scope.
- Advanced inter-procedural dependency tracing for field initializers through called methods is **not implemented yet**.

## Roadmap (next versions)

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

## Opt-out directives

JHarmonizer supports two native comment directives:

- `@jharmonizer:off`
- `@jharmonizer:sort-off`

Directive matching is case-insensitive, so variants such as `@JHarmonizer:OFF` are also recognized.

Supported comment forms:

- line comments: `// @jharmonizer:off`, `// @jharmonizer:sort-off`
- block comments: `/* @jharmonizer:off */`, `/* @jharmonizer:sort-off */`

Supported scopes:

- file scope
- type scope

### File scope

Place a directive in the compilation-unit preamble:

- before `package`
- between `package` and `import`
- between `import` statements and the first top-level type
- at the beginning of a file without `package`

Behavior:

- `@jharmonizer:off` — skip the file completely (no sorting, no formatting, no import fixing)
- `@jharmonizer:sort-off` — skip sorting, but still run formatting and import fixing

### Type scope

Place a directive immediately before a top-level or nested type declaration.

Behavior:

- `@jharmonizer:off` — skip sorting and formatting for that type subtree and preserve its original source text
- `@jharmonizer:sort-off` — skip sorting for that type subtree, but still format it together with the rest of the file

### Unsupported placements and tokens

The following are intentionally unsupported and ignored with a warning:

- member-level directives for fields, methods, constructors, or initializer blocks
- region-based `off/on` markers
- `@jharmonizer:on`, `@jharmonizer:format-off`, `@jharmonizer:format-on`, and similar variants
- directives inside method bodies or inside Javadoc

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
