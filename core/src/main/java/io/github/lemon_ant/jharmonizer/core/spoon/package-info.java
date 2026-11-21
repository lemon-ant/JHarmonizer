// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

/**
 * Shared Spoon utilities and models exchanged between source parsing, sorting, and printing.
 * Reusable utilities here require actual production consumers from different processing stages.
 * Review individual methods as well as whole classes; generic names and potential reuse do not establish shared scope.
 * Helpers consumed within one stage stay beside their owners, even when independent of Spoon.
 * Models may stay beside their producers to restrict initial construction, even when consumed across stages.
 * As an explicit model-construction boundary, {@link RelocationDetector}
 * and its package-private LIS helper stay beside the shared {@link MemberRelocation}
 * model to keep its constructor package-private. Only the sorter invokes the detector; the model crosses stages.
 */
package io.github.lemon_ant.jharmonizer.core.spoon;
