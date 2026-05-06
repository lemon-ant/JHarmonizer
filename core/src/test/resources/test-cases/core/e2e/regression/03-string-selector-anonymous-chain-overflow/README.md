<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# 03-string-selector-anonymous-chain-overflow

This fixture documents and reproduces the anonymous-class initializer shape that previously triggered
unsafe Spoon partial evaluation recursion.

## Observed failure (when guard is removed)

- Error: `java.lang.StackOverflowError`
- Repeating frames include:
  - `spoon.support.reflect.declaration.CtPackageImpl.getQualifiedName(...)`
  - `spoon.support.reflect.eval.VisitorPartialEvaluator.*`
  - `spoon.support.reflect.code.CtNewClassImpl.accept(...)`

## Guard/fix location in production code

- Class: `io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils`
- Method: `findPartiallyEvaluatedExpression(...)`
- Protection: skip `expression.partiallyEvaluate()` when
  `expression.getElements(new TypeFilter<>(CtNewClass.class))` is not empty.

If this protection is removed, this fixture should regress with the documented stack overflow signature.
