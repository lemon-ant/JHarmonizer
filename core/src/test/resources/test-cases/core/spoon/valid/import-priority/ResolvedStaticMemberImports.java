// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package annotationimports;

import static annotationimports.ResolvedStaticMemberImports.Deprecated;
import static annotationimports.ResolvedStaticMemberImports.NestedTag;
import static annotationimports.ResolvedStaticMemberImports.SuppressWarnings;

import java.lang.Deprecated;
import java.lang.SuppressWarnings;

@Deprecated
@SuppressWarnings("unused")
class ResolvedStaticMemberImports {
    static final int Deprecated = 0;

    static void SuppressWarnings() {}

    @interface NestedTag {}
}
