// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package canonicaltypenames;

import canonicaltypenames.library.ExternalAnnotationOwner;

public class CanonicalTypeNameReferences {
    @SourceOwner.Tag int sourceNested;

    @ExternalAnnotationOwner.Tag int externalNested;

    @ExternalAnnotationOwner.Tag$Kind int externalNestedDollar;

    @Top$Tag int topLevelDollar;

    @Dollar$Owner.Tag$Kind int nestedDollar;
}

class SourceOwner {
    @interface Tag {}
}

@interface Top$Tag {}

class Dollar$Owner {
    @interface Tag$Kind {}
}
