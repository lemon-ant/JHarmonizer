# 08-file-change-ingestor-partial-eval-npe

This regression fixture captures a Spoon partial-evaluation `NullPointerException` observed on a NiFi-style
`FileChangeIngestor` source shape with static imports, method references, and map/field access chains.

## Observed failure

- Exception: `java.lang.NullPointerException`
- Message:
  - `Cannot invoke "spoon.reflect.reference.CtTypeReference.getTypeDeclaration()" because the return value of "spoon.reflect.reference.CtFieldReference.getDeclaringType()" is null`
- Key frame:
  - `spoon.support.reflect.eval.VisitorPartialEvaluator.visitFieldAccess(...)`

## Why this matters

This failure happens during `expression.partiallyEvaluate()` while dependency graph code inspects field initializers.
Without guarding, this can fail processing flow on valid source inputs.

## JHarmonizer handling

The same resilience path is used as for other Spoon partial-evaluation failures:

- `io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils`
- method: `findPartiallyEvaluatedExpression(...)`

Runtime exceptions from `partiallyEvaluate()` are caught and escaped, and the logic falls back to raw-expression
behavior (treated as not partially evaluated).

This fixture exists to keep that regression path covered in e2e.
