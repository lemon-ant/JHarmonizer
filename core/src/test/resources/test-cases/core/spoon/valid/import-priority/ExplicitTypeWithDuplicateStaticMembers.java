// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package annotationimports;

import static annotationimports.StaticAnnotationNames.Deprecated;
import static annotationimports.other.OtherStaticAnnotationNames.Deprecated;

import java.lang.Deprecated;

class ExplicitTypeWithDuplicateStaticMembers {
    @Deprecated int simple;

    @java.lang.Deprecated int qualified;
}
