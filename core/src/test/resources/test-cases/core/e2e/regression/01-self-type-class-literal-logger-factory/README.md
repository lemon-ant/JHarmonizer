# 01-self-type-class-literal-logger-factory

This regression fixture documents a Spoon partial-evaluation edge case for self-type class literals.

## What fails

During dependency-graph analysis we may attempt to partially evaluate a field initializer:

- `SelfLoggerFactory.resolve(SelfTypeClassLiteralLoggerFactorySample.class)`

In this e2e environment, Spoon can try to load the declaring type through classpath lookup and throw:

- `spoon.support.SpoonClassNotFoundException: cannot load class: io.github.lemon_ant.jharmonizer.core.e2e.SelfTypeClassLiteralLoggerFactorySample`

Even though the reference points to the same source type, it is not always loadable as a runtime class in this
processing context (the source is being processed, not reliably available as loadable bytecode for Spoon resolution).

## Why this breaks

If this exception is not handled, partial-evaluation flow can fail while computing field/default-value semantics,
which can break sorting flow for the file.

## JHarmonizer handling

Guard location:

- `io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils`
- method: `findPartiallyEvaluatedExpression(...)`

Behavior:

- wraps `expression.partiallyEvaluate()` in exception handling
- catches runtime partial-evaluation failures (including this class-loading case)
- treats expression as "not partially evaluated" (fallback to raw expression path)

So these Spoon class-loading failures are explicitly escaped/neutralized and do not crash the whole pipeline.
