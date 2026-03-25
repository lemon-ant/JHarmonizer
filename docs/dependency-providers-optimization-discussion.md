# Dependency Providers Optimization — обсуждение по шагам

Ниже — список пунктов для поэтапного обсуждения и принятия решений.

## Предложенный список новых corner-case тестов (перед финальным прогоном)

5. `Outer.this.field` / nested `this`-qualification.
6. Compile-time constant edge cases:
   - boxed literals,
   - constant expression через cast/concat,
   - `static final` primitive/String с `partiallyEvaluate` quirks.
