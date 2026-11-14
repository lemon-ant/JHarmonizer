// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package earlymatchlibrary;

public class ExternalAnnotationBase {
    public @interface Tag {
        String value();
    }

    public static class java {
        public static class lang {
            public @interface Deprecated {}
        }
    }
}
