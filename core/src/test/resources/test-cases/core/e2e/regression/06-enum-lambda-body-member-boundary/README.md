# 06-enum-lambda-body-member-boundary

This regression fixture documents a Spoon source-position bug found for enum declarations in a specific shape.

## Bug summary

For this input enum, the **first method after enum constant declarations** (`getDescriptor`) can receive an
incorrect `sourceStart` position from Spoon.

In this scenario the wrong start is shifted into the preceding enum constant area/lambda body, so member boundaries
for the first method are misdetected.

## Why this is important for JHarmonizer

When the start position is wrong, `SpoonTypePrinter` can print source fragments incorrectly (broken member boundaries
produce incorrect rendered code).

## JHarmonizer workaround

To protect source printing, JHarmonizer uses a dedicated correction algorithm:

- `io.github.lemon_ant.jharmonizer.core.translator.spoon.EnumMemberStartCorrectionResolver`

### How the correction algorithm works

1. Collect explicit enum members and select the one with the minimum Spoon-reported `sourceStart`
   (the earliest member in source order).
2. Extract `memberSourceFragment` from original source using Spoon `sourceStart/sourceEnd`.
3. Take the first logical line of `member.toString()` (trimmed) as the target declaration prefix.
4. Convert that first line to a regex pattern with whitespace normalization:
   - whitespace between alphanumeric tokens becomes `\\s+`
   - whitespace near punctuation/boundaries becomes `\\s*`
   - non-whitespace characters are escaped with `Pattern.quote(...)`
5. Search that pattern inside `memberSourceFragment`.
6. If match is found with non-zero offset, compute corrected start as
   `originalSourceStart + matcher.start()` and return correction map for this member.
7. If no offset is detected (no match or match at zero), keep Spoon position unchanged.

This restores the real declaration start even when Spoon points into the preceding enum-constant/lambda region.

## Scope note

This was observed for enum processing in this concrete case and tracked as a Spoon bug/edge case.
