// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@Ax
@Bee
@LongerName
@Short("abcdefghijklmnopqrstuvwxyz")
class AlphabeticalAnnotationOrdering {}

@interface Ax {}

@interface Bee {}

@interface Short {
    String value();
}

@interface LongerName {}
