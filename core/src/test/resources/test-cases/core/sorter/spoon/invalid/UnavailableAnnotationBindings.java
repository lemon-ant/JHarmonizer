// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

import unavailable.library.Deprecated;

class UnavailableAnnotationBindings {

    String @java.lang.annotation.Documented @Deprecated [] importedArray;

    void receiveImported(@java.lang.annotation.Documented @Deprecated UnavailableAnnotationBindings this) {}

    static class Inherited extends unavailable.Base {

        String @unavailable.Base.Tags({}) @Tag("first") [] inheritedArray;

        void receiveInherited(@unavailable.Base.Tags({}) @Tag("first") Inherited this) {}
    }
}

@interface Tag {
    String value();
}
