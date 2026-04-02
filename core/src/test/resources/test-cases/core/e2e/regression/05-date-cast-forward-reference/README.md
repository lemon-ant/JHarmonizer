# 05-date-cast-forward-reference

Minimal regression for `illegal forward reference` caused by reordering exactly two fields:

- `ALTERNATE_FORMAT_WITHOUT_MILLIS` (compile-time constant string)
- `ALTERNATE_FORMATTER_WITHOUT_MILLIS` (uses the constant in initializer)

## Input

Current broken shape: formatter field is above the referenced string constant.

## Expected

Safe shape: string constant is declared before formatter field.
