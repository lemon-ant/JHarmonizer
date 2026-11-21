// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package annotationqualifiedfallback;

import annotationqualifiedfallback.library.*;
import lombok.*;

@lombok.EqualsAndHashCode
class QualifiedAnnotationFallback {
    @java.lang.Deprecated
    int matchingName;

    String @ExternalOwner.Tag [] absentBinding;

    @EqualsAndHashCode.Exclude
    int suffixOnlyName;
}
