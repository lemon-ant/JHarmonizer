# Java Sorter Configuration DSL

This document describes the structure and semantics of the YAML-based DSL used to define rules for sorting Java class members and file-level elements. The goal is to provide a clear, flexible, and declarative way to enforce consistent class structures across Java projects.

---

## 🔹 File-Level Configuration

```yaml
java-file:
  main-type-first: true
  type-order:
    - public-class
    - class
    - interface
    - enum
    - record
```

### Parameters:

* `main-type-first`: If `true`, the public type matching the filename is placed first.
* `type-order`: Defines the order of top-level types in the `.java` file.

Values can be:

* `public-class`
* `class`
* `interface`
* `enum`
* `record`

---

## 🔹 Selector Aliases

```yaml
selector-aliases:
  object-methods: '~^(toString\(\)|equals\(\)|hashCode\(\)|clone\(\)|finalize\(\))$'
  junit-init:
    - '@BeforeEach'
    - '@BeforeAll'
  junit-cleanup:
    - '@AfterEach'
    - '@AfterAll'
  test-class: "~.*Test$"
```

### Purpose:

Defines reusable shortcuts for commonly used patterns, annotations, or regular expressions.

* Regular expressions must be quoted and prefixed with `~`.
* Aliases can be a string or list.

---

## 🔹 Type-Specific Sorting Rules

```yaml
type-sort:
  <rule-name>:
    match: <selector-map> | <selector-alias> | [list-of-selectors]
    behavior: <optional-behavior>
    sort:
      <member-type>:
        <selector>: <strategy>
        rest: <strategy>
```

### Example

```yaml
  test:
    match: test-class
    sort:
      method:
        junit-init: orig
        '@Test': alpha
        junit-cleanup: orig
        rest: orig
```

### Match Block

* **Map form (AND)**:

```yaml
match:
  modifiers: [final, static]
  name: "~.*Util$"
```

All conditions must match.

* **List form (OR)**:

```yaml
match:
  - modifiers: [final, static]
  - name: "~.*Util$"
```

At least one condition must match.

You can also use a **single selector alias**:

```yaml
match: test-class
```

---

### Member Types

Under `sort`, you can define rules per member type:

* `field`
* `method`
* `constructor`
* `initializer`
* `type` (for inner classes/enums/interfaces)

### Sorting Strategy Values

* `alpha`: Alphabetical order
* `orig`: Preserve original source code order (origin)
* `rest`: Fallback bucket for unmatched elements

---

### Optional Behavior Block

```yaml
behavior:
  keepAccessorsTogether: true
```

Supported flags:

* `keepAccessorsTogether`: Places getters/setters in the same group, even if named differently.

### Group option inheritance (`type-members-ordering`)

For nested member groups, the following options are inherited from the nearest parent that defines them:

* `keepAccessorsTogether`
* `separator`
* `ordering-rules`

Inheritance is resolved top-down in the group tree:

* if a child omits an option, it inherits the parent's resolved value;
* if a child defines an option explicitly, it overrides the inherited value for its subtree.

For `ordering-rules`, an explicit child value fully replaces the inherited list.
An empty `ordering-rules: []` is allowed and means “no explicit sort keys at this level”.

---

## 🔹 Example: Full Configuration

```yaml
java-file:
  main-type-first: true
  type-order:
    - public-class
    - class
    - interface
    - enum
    - record

selector-aliases:
  object-methods: '~^(toString\(\)|equals\(\)|hashCode\(\)|clone\(\)|finalize\(\))$'
  junit-init:
    - '@BeforeEach'
    - '@BeforeAll'
  junit-cleanup:
    - '@AfterEach'
    - '@AfterAll'
  test-class: "~.*Test$"

type-sort:
  test:
    match: test-class
    sort:
      method:
        junit-init: orig
        '@Test': alpha
        junit-cleanup: orig
        rest: orig

  utility:
    match:
      - [final, static]
      - "~.*Utils?$"
    sort:
      field:
        static: alpha
        instance: alpha
      method: alpha

  dto:
    match: [final, "~.*Dto$"]
    behavior:
      keepAccessorsTogether: true
    sort:
      field:
        instance: alpha
      method: alpha

  fallback:
    behavior:
      keepAccessorsTogether: true
    sort:
      field:
        static:
          public: alpha
          private: orig
        instance:
          public: alpha
          private: orig
      method:
        '@Override': orig
        object-methods: orig
        rest: alpha
```

---

## 🧠 Notes

* Rule order in `type-sort` matters. First matching rule wins.
* `rest` is optional but recommended.
* The `fallback` rule (instead of `default`) avoids clashing with the Java `default` keyword.
* Aliases improve maintainability and reduce duplication.

### Merging custom `type-members-ordering` with the default model

When a custom configuration is applied on top of the embedded default configuration, root groups from
`type-members-ordering` are merged by **exact root-group name**:

* if the custom root-group name matches a default root-group name, the default root group is fully replaced;
* the replacement stays on the original default position, even if the custom file defines it in another place;
* if the custom root-group name is new, that root group is inserted before all default root groups;
* several new custom root groups keep their relative order from the custom file;
* nested `groups:` blocks are not merged recursively — replacing a root group replaces its whole subtree.

This means you can override only one named root group and keep the rest of the default model unchanged.

---

For questions or feature suggestions, open an issue or contact the maintainer.
