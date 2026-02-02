# JHarmonizer default rule — member ordering specification (draft)

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

### Alpha sort key

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

## 1) Fields

### 1.1 Static fields

#### 1.1.1 Static final fields (constants)
1. `serialVersionUID` (if present) — first
2. Logger fields (if present) — next
3. All other static final fields — sorted by:
   - **visibility**
   - **ALPHA**

#### 1.1.2 Static non-final fields
Sorted by:
- **visibility**
- **ALPHA**

### 1.2 Instance fields

#### 1.2.1 Instance final fields
Sorted by:
- **visibility**
- **ALPHA**

#### 1.2.2 Instance non-final fields
Sorted by:
- **visibility**
- **ALPHA**

---

## 2) Initializers

### 2.1 Static initializers
- order: **preserve/source order**

### 2.2 Instance initializers
- order: **preserve/source order**

---

## 3) Methods (including constructors)

> Note: `keepAccessorsTogether = ON` at this level; inherited by all method subgroups.

Methods are first split into explicit **visibility groups** (the order is defined structurally in config, not by a sort-key):
1. `public`
2. `protected`
3. `package-private`
4. `private`

Inside **each** visibility group, members are ordered as three sequential subgroups:

1. **Static methods** — sort: **ALPHA**
2. **Constructors** — sort: **ALPHA**
3. **Instance methods** — sort: **ALPHA**

---

## 4) Nested types

Nested types are first split into explicit **visibility groups** (the order is defined structurally in config, not by a sort-key):
1. `public`
2. `protected`
3. `package-private`
4. `private`

Inside **each** visibility group, nested type declarations are ordered by kind:

1. **Annotations** (`@interface`) — sort: **ALPHA**
2. **Enums** — sort: **ALPHA**
3. **Records** — sort: **ALPHA**
4. **Interfaces** — sort: **ALPHA**
5. **Classes** — sort: **ALPHA**

No additional split by `static` vs `non-static` is applied for nested types; ordering is purely kind order + ALPHA.

---

## Notes

- This spec intentionally focuses only on the Default Rule (fallback root group).
- Specialized root groups (tests, DTO/entities, etc.) are out of scope for this draft.
