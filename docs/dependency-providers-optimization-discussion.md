# Dependency Providers Optimization — обсуждение по шагам

Ниже — список пунктов для поэтапного обсуждения и принятия решений.

## Предложенный список новых corner-case тестов (перед финальным прогоном)

2. Method reference in initializer с доступом к полю.
3. Anonymous class in initializer с чтением поля outer-type.
4. Enum + static field mixed initialization.
5. `Outer.this.field` / nested `this`-qualification.
6. Compile-time constant edge cases:
   - boxed literals,
   - constant expression через cast/concat,
   - `static final` primitive/String с `partiallyEvaluate` quirks.
