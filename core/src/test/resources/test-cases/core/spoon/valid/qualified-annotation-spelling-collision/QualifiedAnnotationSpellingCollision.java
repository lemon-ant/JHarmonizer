// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package EqualsAndHashCode;

import lombok.*;

class QualifiedAnnotationSpellingCollision {
    @EqualsAndHashCode.Exclude
    int value;
}

class Exclude {}
