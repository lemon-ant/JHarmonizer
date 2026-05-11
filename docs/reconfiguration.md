<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Reconfiguring JHarmonizer

JHarmonizer is designed to start with useful embedded defaults and then accept a
small YAML overlay for project-specific rules. You usually do **not** need to copy
the full default configuration: put only the settings or root member groups you
want to change into your own file.

For the complete YAML syntax, selector tokens, ordering rules, and separator values,
see the [configuration DSL reference](config-dsl.md).

## Fast path for Maven projects

For Maven, the quickest setup is to create a `jharmonizer.yml` file in the project
root, next to `pom.xml`:

```text
my-project/
├── pom.xml
├── jharmonizer.yml
└── src/
```

The Maven plugin checks `${project.basedir}/jharmonizer.yml` by default. If the file
exists, it is loaded automatically and merged over the built-in defaults. If it does
not exist, the plugin simply uses the embedded default configuration.

If your repository uses another file name, for example `JHarmonizer.yaml`, point the
plugin at it explicitly:

```xml
<plugin>
    <groupId>io.github.lemon-ant.jharmonizer</groupId>
    <artifactId>jharmonizer-maven-plugin</artifactId>
    <version>1.0.1</version>
    <configuration>
        <configFile>${project.basedir}/JHarmonizer.yaml</configFile>
    </configuration>
</plugin>
```

You can also override the path for a single run:

```bash
mvn jharmonizer:reorder -Djharmonizer.configFile=JHarmonizer.yaml
```

## Minimal overlay examples

### Disable backups and detailed statistics

```yaml
# SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
# SPDX-License-Identifier: Apache-2.0
backups-enabled: false
processing-statistics-mode: DISABLED
```

### Change only formatting behavior

```yaml
# SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
# SPDX-License-Identifier: Apache-2.0
formatting:
  fix-imports: true
  blank-line-between-fields: true
```

Omitted formatting keys keep their default values.

### Add a project-specific root member group

The following overlay inserts a new root group before the default root groups. It
collects Spring controller classes and sorts their methods by visibility and name,
while all other classes continue to use the embedded default rules.

```yaml
# SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
# SPDX-License-Identifier: Apache-2.0
type-members-ordering:
  - name: Spring Controllers
    includes:
      - '@Controller'
      - '@RestController'
    ordering-rules: [ visibility-desc, alpha ]
    separator: new-line
    groups:
      - name: Fields
        includes: field
      - name: Constructors
        includes: constructor
      - name: Request Handlers
        includes:
          - '@~.*Mapping$'
      - name: Other Methods
        includes: method
```

### Replace one default root group

Root member groups are merged by `name`. If an overlay defines a root group with the
same name as a default root group, the custom group replaces that default group in
its original position.

```yaml
# SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
# SPDX-License-Identifier: Apache-2.0
type-members-ordering:
  - name: Default Rule
    includes: ~.*
    ordering-rules: preserve
    groups:
      - name: Constants
        includes: [ field, static, final ]
        ordering-rules: alpha
      - name: Fields
        includes: field
        ordering-rules: [ visibility-desc, alpha ]
      - name: Constructors
        includes: constructor
      - name: Methods
        includes: method
        ordering-rules: [ visibility-desc, alpha ]
      - name: Nested Types
        includes:
          - class
          - interface
          - enum
          - annotation
          - record
        ordering-rules: alpha
```

## How overlay merging works

JHarmonizer builds the active configuration in layers:

1. Load the embedded `default-config.yml` from `jharmonizer-core`.
2. Load the optional YAML overlay (`jharmonizer.yml` in Maven by default, or the
   path passed through Maven/CLI parameters).
3. Merge the overlay over the defaults.
4. Apply explicit Maven/CLI parameter overrides, such as `backupsEnabled` and
   `processingStatisticsMode`.
5. Compile the merged configuration into the runtime model used by the sorter and
   formatter.

Merge rules are intentionally simple:

- scalar values such as `backups-enabled` and `processing-statistics-mode` replace
  the default only when present;
- nested blocks such as `formatting`, `header-line`, and `top-level-types-ordering`
  are merged field by field;
- `type-members-ordering` root groups are merged by `name`;
- a custom root group with an existing default name replaces that full root group;
- a custom root group with a new name is inserted before all default root groups;
- nested `groups:` are not merged recursively: replacing a root group replaces its
  whole subtree.

These rules make small overlays safe: you can tune one setting, add one special group,
or replace one named root group without duplicating the whole default configuration.

## CLI equivalent

The CLI uses the same overlay format, but it does not auto-discover a root file. Pass
the configuration path explicitly:

```bash
java -jar jharmonizer-cli-1.0.1.jar reorder --base-dir src/main/java --config jharmonizer.yml
```

See [`cli/README.md`](../cli/README.md) for all CLI options and exit codes.
