# Lazy Context Filter Impact Analysis (Dependency Providers)

## Scope
Analyzed the impact of this filter in `DeclaringTypeFieldReferenceUtils.streamFieldAccessesInSameType(...)`:

```java
.filter(fieldAccess -> !isInsideLazyContext(declaringType, memberAstRoot, fieldAccess))
```

The filter is currently applied to all field-access scans used by dependency providers.

---

## Where the lazy-context filter is used
The filter is in a shared scanner method and therefore affects all callers of:

- `findProviderFieldsRequiredByDependentMember(...)`
- `findFieldsReadByMember(...)`
- `findFieldsWrittenByMember(...)`

This means it impacts the following providers:

### Directly affected providers
1. `FieldInitializerBackwardReferenceDependencyProvider`
   - via `findProviderFieldsRequiredByDependentMember(...)`
2. `InitializerBlockDependencyProvider`
   - via `findProviderFieldsRequiredByDependentMember(...)`
3. `EnumConstantInitializerDependencyProvider`
   - via `findProviderFieldsRequiredByDependentMember(...)`
4. `BlankFinalDefiniteAssignmentDependencyProvider`
   - via `findFieldsReadByMember(...)`
   - and indirectly via `InitializationOrderDependencyUtils.assignsField(...)` -> `findFieldsWrittenByMember(...)`

### Not affected providers
- `AccessorPairDependencyProvider`
- `ExplicitThisInitializerFieldDependencyProvider`
- `ExplicitDeclaringTypeInitializerFieldDependencyProvider`

These do not use the shared field-access scanner above.

---

## Per-provider usefulness analysis

## 1) FieldInitializerBackwardReferenceDependencyProvider
**Verdict: clearly useful.**

Reason:
- Field initializer dependencies should reflect eager initialization semantics.
- Field accesses inside lambda/method-reference/local/nested type bodies are lazy/deferred and must not force provider-before-dependent ordering.
- Without the filter, false declaration dependencies are created (confirmed by lazy lambda E2E fixture).

Practical effect:
- Prevents over-constraining reorder decisions.
- Avoids pinning unrelated providers before dependent fields when access happens only at runtime of a deferred closure.

## 2) InitializerBlockDependencyProvider
**Verdict: useful.**

Reason:
- Same semantics as field initializers, but for init blocks.
- Init block body may contain deferred contexts (lambda, method references, nested types).
- Those accesses are not part of immediate initialization order constraints.

Practical effect:
- Reduces false declaration dependencies from deferred code inside `{ ... }` / `static { ... }` blocks.

## 3) EnumConstantInitializerDependencyProvider
**Verdict: useful.**

Reason:
- Enum constants are static initialization members, but their initializer expressions can include lazy/deferred execution contexts.
- Accesses inside those contexts should not create eager declaration dependencies.

Practical effect:
- Avoids unnecessary ordering constraints between enum constants / fields due to runtime-only access paths.

## 4) BlankFinalDefiniteAssignmentDependencyProvider (read side)
**Verdict: useful.**

Reason:
- This provider adds conservative edges for blank-final reads.
- Reads from deferred contexts do not necessarily represent immediate reads during initialization and should not trigger additional assignment-provider edges.

Practical effect:
- Keeps the conservative algorithm focused on truly eager reads.
- Reduces spurious edges that are not needed for compile-time definite-assignment safety.

## 5) BlankFinalDefiniteAssignmentDependencyProvider (write side via `findFieldsWrittenByMember`)
**Verdict: mostly redundant on valid Java sources.**

Reason:
- The write-side scanner is used to detect assignment providers for blank-final fields.
- Assigning a blank-final field from lazy contexts (for example lambda body) is not a valid practical provider in compilable code and typically leads to compile-time errors.
- Therefore, excluding lazy-context writes usually has no observable effect for valid inputs.

Practical effect:
- Filter is harmless here, but typically transparent.
- This is the strongest candidate for “logically excessive but safe”.

---

## Consolidated classification

## Clearly useful (real-world false-positive prevention)
- `FieldInitializerBackwardReferenceDependencyProvider`
- `InitializerBlockDependencyProvider`
- `EnumConstantInitializerDependencyProvider`
- `BlankFinalDefiniteAssignmentDependencyProvider` (read detection path)

## Mostly transparent / usually redundant
- `BlankFinalDefiniteAssignmentDependencyProvider` write-detection path (`assignsField` -> `findFieldsWrittenByMember`)

## Not using this logic at all
- `AccessorPairDependencyProvider`
- `ExplicitThisInitializerFieldDependencyProvider`
- `ExplicitDeclaringTypeInitializerFieldDependencyProvider`

---

## Recommendation
1. Keep the lazy-context filter enabled globally for scanner correctness and simplicity.
2. Optional micro-optimization: add an explicit comment near blank-final write detection noting that the lazy filter is expected to be mostly redundant for valid compilable sources.
3. If desired, add a focused unit test for blank-final write detection in deferred contexts to document current intended behavior.
