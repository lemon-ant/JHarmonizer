# Dependency Providers Optimization — обсуждение по шагам

Ниже — список пунктов для поэтапного обсуждения и принятия решений.

## A) LazyInitializerContextPruningProvider (только как fallback-опция)

**Идея**
- После сбора dependency по initializer-root удалять рёбра,
  пришедшие из lazy/non-eager поддеревьев:
  - lambda
  - method references
  - local type body

**Комментарий к подходу (после ревью)**
- Предпочтительный путь: оптимизировать действующие providers,
  чтобы лишние зависимости не создавались изначально.
- `LazyInitializerContextPruningProvider` допустим только как fallback,
  если часть edge-cases технически неудобно выразить в текущих providers.
- Если fallback всё же используется, он должен быть:
  - явно ограничен только lazy/non-eager контекстами,
  - покрыт отдельными тестами на отсутствие регрессий,
  - документирован как временный слой, а не основной механизм.

---

## C) InitializerStaticnessGuardProvider

**Проблема**
- Общий reference collector фильтрует в первую очередь по
  `declaring type + source order`,
  но не делает явный staticness-guard на этом этапе.

**Что можно добавить**
- Дополнительный статический/инстансный контекстный фильтр
  для initializer members.

---

## Предложенный список новых corner-case тестов (перед финальным прогоном)

1. Lazy lambda in field initializer:
   - `int a = (() -> b).get(); int b = 1;`
   - Проверить, что dependency не пере-консервативный.
2. Method reference in initializer с доступом к полю.
3. Anonymous class in initializer с чтением поля outer-type.
4. Enum + static field mixed initialization.
5. `Outer.this.field` / nested `this`-qualification.
6. Compile-time constant edge cases:
   - boxed literals,
   - constant expression через cast/concat,
   - `static final` primitive/String с `partiallyEvaluate` quirks.

---
