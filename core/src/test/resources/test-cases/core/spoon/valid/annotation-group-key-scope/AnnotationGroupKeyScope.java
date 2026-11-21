// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package annotationgroupscope;

class AnnotationGroupKeyScope {

    @Envelope({@Nested})
    @Marker
    int first;

    @Nested
    int second;
}

@interface Envelope {
    Nested[] value();
}

@interface Marker {}

@interface Nested {}
