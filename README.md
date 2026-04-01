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

- `@jharmonizer:fully-off`
- `@jharmonizer:sort-off`

Directive matching is case-insensitive, so variants such as `@JHarmonizer:Fully-Off` are also recognized.

Supported comment forms:

- line comments: `// @jharmonizer:fully-off`, `// @jharmonizer:sort-off`
- block comments: `/* @jharmonizer:fully-off */`, `/* @jharmonizer:sort-off */`

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

- `@jharmonizer:fully-off` — fully disable harmonization for the file (no sorting, no formatting, no import fixing)
- `@jharmonizer:sort-off` — disable sorting only, but still run formatting and import fixing

### Type scope

Place a directive immediately before a top-level or nested type declaration.

Behavior:

- `@jharmonizer:fully-off` — fully disable harmonization for that type subtree and preserve its original source text
- `@jharmonizer:sort-off` — disable sorting only for that type subtree, but still format it together with the rest of the file

### Unsupported placements and tokens

The following are intentionally unsupported and ignored with a warning:

- member-level directives for fields, methods, constructors, or initializer blocks
- region-based `off/on` markers
- `@jharmonizer:on`, `@jharmonizer:format-off`, `@jharmonizer:format-on`, and similar variants
- directives inside method bodies or inside Javadoc

## Maven/archetype template placeholders (`package ${package};`)

Files that still contain template placeholders such as `package ${package};` are not valid Java source yet. In this
situation the Palantir formatter cannot parse them; JHarmonizer leaves such files unmodified, marks them with an
ERROR result, and logs a warning, but the overall run continues and the CLI still exits with status 0.

For template sources you have two supported options:

1. Add `// @jharmonizer:fully-off` as the first line of that template file to skip harmonization for the whole file
   (recommended when you want clean runs without formatter warnings/errors).
2. Leave the file without opt-out and accept that each run will report a per-file ERROR and warning for that file
   because the formatter cannot parse the non-Java template placeholders; the file will be skipped and left unmodified.
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
