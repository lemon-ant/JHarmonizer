<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Printer fixtures

`scenarios/` follows the shared E2E convention: numbered directories containing
`input/`, `expected/`, and `config.yml`. `SrcPrinterE2ETest` discovers each input
automatically, compiles both versions, compares the complete output, and checks
that processing it again requires no changes. Formatting and import cleanup are disabled.

Java test methods cover only line-ending and whitespace transformations that do
not fit literal fixtures. The local EditorConfig preserves significant comment whitespace.

Scenarios `08`–`10` and `16` preserve the original tail after the last source type, following one blank
line. Expected comments retain their literal spelling and spacing. Variants cover absent or repeated
final terminators, trailing whitespace and terminal ASCII SUB, including its Unicode escape.
