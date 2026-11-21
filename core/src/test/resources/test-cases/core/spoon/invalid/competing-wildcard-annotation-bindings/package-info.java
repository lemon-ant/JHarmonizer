// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@Tag
package annotationwildcardorder;

import static annotationwildcardorder.AvailableWildcardOwner.*;
import static annotationwildcardorder.UnavailableWildcardOwner.*;

class AvailableWildcardOwner {
    @interface Tag {}
}

class UnavailableWildcardOwner extends wildcardorderlibrary.EmptyAnnotationBase {}
