# JHarmonizer default rule — member ordering specification (draft)

## Tree view (single hierarchy)

```
Default Rule (fallback root group)
├─ 1) Fields
│  ├─ 1.1) Static fields
│  │  ├─ 1.1.1) Static final fields (constants)
│  │  │  ├─ 1.1.1.1) serialVersionUID (if present) — first
│  │  │  ├─ 1.1.1.2) Logger fields (if present) — next
│  │  │  └─ 1.1.1.3) Other static final fields — sort: visibility → ALPHA
│  │  └─ 1.1.2) Static non-final fields — sort: visibility → ALPHA
│  └─ 1.2) Instance fields
│     ├─ 1.2.1) Instance final fields — sort: visibility → ALPHA
│     └─ 1.2.2) Instance non-final fields — sort: visibility → ALPHA
├─ 2) Initializers
│  ├─ 2.1) Static initializers — preserve/source order
│  └─ 2.2) Instance initializers — preserve/source order
├─ 3) Methods (including constructors)
│  ├─ note: keepAccessorsTogether = ON (applied here; inherited by all subgroups)
│  ├─ 3.1) Public
│  │  ├─ Static methods — sort: ALPHA
│  │  ├─ Constructors — sort: ALPHA
│  │  └─ Instance methods — sort: ALPHA
│  ├─ 3.2) Protected
│  │  ├─ Static methods — sort: ALPHA
│  │  ├─ Constructors — sort: ALPHA
│  │  └─ Instance methods — sort: ALPHA
│  ├─ 3.3) Package-private
│  │  ├─ Static methods — sort: ALPHA
│  │  ├─ Constructors — sort: ALPHA
│  │  └─ Instance methods — sort: ALPHA
│  └─ 3.4) Private
│     ├─ Static methods — sort: ALPHA
│     ├─ Constructors — sort: ALPHA
│     └─ Instance methods — sort: ALPHA
└─ 4) Nested types
   ├─ 4.1) Public
   │  ├─ Annotations (@interface) — sort: ALPHA
   │  ├─ Enums — sort: ALPHA
   │  ├─ Records — sort: ALPHA
   │  ├─ Interfaces — sort: ALPHA
   │  └─ Classes — sort: ALPHA
   ├─ 4.2) Protected
   │  ├─ Annotations (@interface) — sort: ALPHA
   │  ├─ Enums — sort: ALPHA
   │  ├─ Records — sort: ALPHA
   │  ├─ Interfaces — sort: ALPHA
   │  └─ Classes — sort: ALPHA
   ├─ 4.3) Package-private
   │  ├─ Annotations (@interface) — sort: ALPHA
   │  ├─ Enums — sort: ALPHA
   │  ├─ Records — sort: ALPHA
   │  ├─ Interfaces — sort: ALPHA
   │  └─ Classes — sort: ALPHA
   └─ 4.4) Private
      ├─ Annotations (@interface) — sort: ALPHA
      ├─ Enums — sort: ALPHA
      ├─ Records — sort: ALPHA
      ├─ Interfaces — sort: ALPHA
      └─ Classes — sort: ALPHA
```

---

## Scope

This specification describes the **Default Rule** ordering (the fallback root member group that applies when no specialized root group matches).

It defines:
- the **top-level sequence** of member groups inside a type;
- the **subgroup structure** for each group;
- the **sorting strategy** inside each subgroup.

## Definitions

### Visibility

Visibility levels are:
- `public`
- `protected`
- `package-private`
- `private`

> Note: for Methods and Nested Types, visibility is modeled as **explicit structural groups** (not a sort-key).
> The exact visibility group order is determined by configuration structure.

### Alpha ordering rule

**ALPHA** is a deterministic key that includes *all relevant signature parts* in one string:
- for a **field**: `fieldName + ":" + fieldType`
- for a **method**: `methodName + "(" + parameterTypes + ")" + ":" + returnType`
- for a **constructor**: `constructorName + "(" + parameterTypes + ")"`
  (constructor name is the declaring type name; parameters drive the order)

This means overloaded methods/constructors are naturally ordered by their parameter list as part of ALPHA.

### Preserve / source order

When a member kind has no stable ALPHA key (e.g., initializer blocks), order is **preserved** (original source order), unless dependency constraints force otherwise.

### Accessor co-location

For the **Methods** subtree, `keepAccessorsTogether = ON` is applied at the group level so it is inherited by all nested method subgroups.

## Non-negotiable constraints

1. **Compilation safety** must be preserved.
2. **Declaration dependencies** must be respected (provider before dependent).
3. **Accessor co-location** (if enabled) must be preserved.
4. If constraints conflict with the preferred visual order, constraints win.

---

## Default Rule ordering (top → bottom)

1. **Fields**
2. **Initializers**
3. **Methods** (including constructors)
4. **Nested types**

---

## Notes

- This spec intentionally focuses only on the Default Rule (fallback root group).
- Specialized root groups (tests, DTO/entities, etc.) are out of scope for this draft.
