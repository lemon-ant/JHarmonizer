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

## D) Fixture consolidation and de-duplication plan

**Проблема**
- В e2e fixture-наборе есть риск семантических пересечений и почти дублирующих сценариев.
- Из-за этого растут количество папок, стоимость поддержки и время ревью.

**Что пересмотреть**
- Провести ревизию всех fixture-папок (включая parked `.~java`) и выявить:
  - полные дубли,
  - сильные пересечения по смыслу,
  - сценарии, которые можно объединить без потери coverage.
- По возможности:
  - слить похожие кейсы в один,
  - перенести похожие фикстуры в уже существующие папки,
  - снизить количество нумерованных папок с сохранением читаемости и coverage.

**Ожидаемый результат**
- Меньше повторений и перекрытий.
- Более компактная и понятная структура тест-кейсов.
- Более дешёвая поддержка fixture-набора при том же уровне регрессионной защиты.

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
