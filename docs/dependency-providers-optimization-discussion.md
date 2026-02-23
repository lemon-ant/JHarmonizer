# Dependency Providers Optimization — обсуждение по шагам

Ниже — список пунктов для поэтапного обсуждения и принятия решений.

---

## 1) Слишком широкий сбор ссылок в initializer roots (главный кандидат на next optimization)

**Текущее поведение**
- `findReferencedFields()` идёт через `CtFieldAccess` по всему `AST-root`.
- Добавляется dependency, если provider выше по source order.
- Подход безопасный, но пере-консервативный (даёт лишние жёсткие рёбра).

**Что добавить / изменить**
- Исключать lazy-contexts из обхода:
  - `CtLambda`
  - method references
  - тела локальных/анонимных/вложенных типов внутри инициализаторов
- Разделять read/write:
  - `LHS assignment` не должен создавать read-like dependency.
- Рассмотреть учёт compile-time constants в general provider
  - не только в explicit-declaring-type provider.

**Ожидаемый эффект**
- Снижение количества жёстких рёбер.
- Меньше искусственных циклов.

---

## 3) Blank final provider сейчас intentionally conservative — можно сделать точнее

**Текущее поведение**
- При чтении `blank final` добавляются зависимости от **всех** верхних initialization members,
  которые пишут в поле.
- Это безопасно, но может лишне «склеивать» порядок.

**Вариант улучшения**
- `optimized` режим:
  - учитывать только ближайший гарантированный provider по линейному init-order.

**Обязательно покрыть тестами**
- Множественные записи в `blank final`.
- Разделение `static/instance` сценариев.

---

## 4) Explicit forward providers покрывают `this.field` и `Type.field`, но не все квалифицированные формы

**Текущее поведение**
- Explicit-логика опирается на qualifier matchers:
  - `this` (не implicit)
  - явный `TypeAccess` (не implicit)

**Corner-cases для проверки/добавления**
- `OuterClass.this.field` внутри inner class initializer.
- Generic-qualified type access и nested type access.
- Сценарии, где Spoon даёт implicit target иначе, чем ожидается.

---

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
4. Field write only (`LHS`) внутри init block — не должен считаться read dependency.
5. Enum + static field mixed initialization.
6. Blank final multiple assignments candidates (проверить точность/избыточность).
7. `Outer.this.field` / nested `this`-qualification.
8. Compile-time constant edge cases:
   - boxed literals,
   - constant expression через cast/concat,
   - `static final` primitive/String с `partiallyEvaluate` quirks.

---

## Формат обсуждения

Предлагаю проходить последовательно:
1. Уточняем ожидаемую семантику.
2. Фиксируем правило для провайдера.
3. Фиксируем тест-кейсы.
4. Только после этого идём в имплементацию.
