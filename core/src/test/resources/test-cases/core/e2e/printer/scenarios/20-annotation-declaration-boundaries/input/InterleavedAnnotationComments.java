// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@Deprecated
// Top-level type annotations.
@SuppressWarnings("unused")
class ZuluAnnotatedType {}

class InterleavedAnnotationComments {
    @Deprecated
    // Field annotations.
    @SuppressWarnings("unused")
    int value;
    @Deprecated
    // Constructor annotations.
    @SuppressWarnings("unused")
    InterleavedAnnotationComments() {}
    @Deprecated
    // Method annotations.
    @SuppressWarnings("unused")
    void execute() {}
    @Deprecated
    // Nested type annotations.
    @SuppressWarnings("unused")
    class NestedAnnotatedType {}
}
