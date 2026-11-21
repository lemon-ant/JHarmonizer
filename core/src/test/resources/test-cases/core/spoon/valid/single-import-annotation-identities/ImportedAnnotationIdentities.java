// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package annotationimportidentities;

import annotationimportdependencies.ImportedOnlyTag;
import annotationimportidentities.ImportSource.Owner;
import annotationimportidentities.ImportSource.Tag;
import java.lang.Deprecated;

class ImportedAnnotationIdentities {
    @Tag
    int sourceAnnotation;

    @Owner.NestedTag
    int sourceOwner;

    @ImportedOnlyTag
    int unavailableAnnotation;

    @Deprecated
    int platformAnnotation;
}

class ImportSource {
    @interface Tag {}

    static class Owner {
        @interface NestedTag {}
    }
}
