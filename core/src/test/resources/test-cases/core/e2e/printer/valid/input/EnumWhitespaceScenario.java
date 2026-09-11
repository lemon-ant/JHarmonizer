// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
enum EnumWhitespaceScenario {
    FIRST, SECOND;

    // Keep the  double spacing.<trailing-whitespace>
    private static final String LABEL = "two  spaces";

    String zebra() { return "three   spaces"; }
    String alpha() { return LABEL; }
}
