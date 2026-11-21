// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@Nested.Tag
package annotationwildcardmembers;

import static annotationwildcardmembers.WildcardImportSource.Nested.*;
import static annotationwildcardmembers.WildcardImportSource.Other.*;

class WildcardImportSource {
    static class Nested {
        @interface Tag {}
    }

    static class Other {
        static class Nested {
            @interface Tag {}
        }
    }
}
