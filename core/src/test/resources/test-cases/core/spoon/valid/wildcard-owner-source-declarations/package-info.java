// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@Nested.Tag
package annotationwildcarddeclarations;

import static annotationwildcarddeclarations.WildcardAnnotationOwners.*;
import static java.lang.annotation.ElementType.*;

class WildcardAnnotationOwners {
    static class Nested {
        @interface Tag {}
    }
}
