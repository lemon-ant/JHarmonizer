<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# 02-enum-lambda-body-member-boundary

This fixture preserves a historical Spoon source-position regression: the first method after
the enum constants could start inside the preceding constant's lambda body. Incorrect boundaries
caused source-fragment printing to split the declaration incorrectly.

With Spoon 11.5.0 and LF endings, `getDescriptor` starts at offset 580, matching the source declaration.
`SpoonSrcPrinter` uses those positions directly. The former correction based on `toString()`
and a declaration-prefix regex has been removed. Keep the input and expected output unchanged
to detect regressions in future Spoon versions.
