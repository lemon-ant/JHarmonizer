# 08-file-change-ingestor-partial-eval-npe

This is a **minimal** regression fixture for a Spoon partial-evaluation `NullPointerException` seen in NiFi-style
`FileChangeIngestor` code.

## Why this fixture is intentionally small

The previous version used a full production-like class and was too large/noisy.
This reduced fixture keeps only the expression shape needed to trigger the same failure mode:

- static imported external constants (`NOTIFIER_INGESTORS_KEY`, `WHOLE_CONFIG_KEY`)
- `Map.of(...)` field initializer using method reference (`WholeConfigDifferentiator::getByteBufferDifferentiator`)

## Observed failure

- Exception: `java.lang.NullPointerException`
- Message:
  - `Cannot invoke "spoon.reflect.reference.CtTypeReference.getTypeDeclaration()" because the return value of "spoon.reflect.reference.CtFieldReference.getDeclaringType()" is null`
- Key frame:
  - `spoon.support.reflect.eval.VisitorPartialEvaluator.visitFieldAccess(...)`

## JHarmonizer handling

This is handled in:

- `io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils`
- `findPartiallyEvaluatedExpression(...)`

When partial evaluation throws runtime exceptions (including this NPE case), the logic falls back to
"not partially evaluated" behavior (raw expression path), so pipeline processing continues.
