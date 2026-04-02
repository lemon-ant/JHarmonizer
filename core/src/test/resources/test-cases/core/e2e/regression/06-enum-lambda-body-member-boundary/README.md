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

The resolver detects this enum-specific offset and corrects member start positions before printing.

## Scope note

This was observed for enum processing in this concrete case and tracked as a Spoon bug/edge case.
