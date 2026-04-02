# 05-date-cast-forward-reference

Regression fixture for field reordering that can introduce `illegal forward reference` in a NiFi-like
`DateCastEvaluator` constants block.

## Problem shape

The class has static formatter fields that reference string format constants declared in the same type:

- `ALTERNATE_FORMATTER_WITHOUT_MILLIS` -> `ALTERNATE_FORMAT_WITHOUT_MILLIS`
- `ALTERNATE_FORMATTER_WITH_MILLIS` -> `ALTERNATE_FORMAT_WITH_MILLIS`
- `DATE_TO_STRING_FORMATTER` -> `DATE_TO_STRING_FORMAT`

If reorder moves formatter fields above those constants, Java compilation fails with
`illegal forward reference`.

## Regression expectation

After processing, constant declaration order must still keep referenced format strings above dependent formatter fields.
