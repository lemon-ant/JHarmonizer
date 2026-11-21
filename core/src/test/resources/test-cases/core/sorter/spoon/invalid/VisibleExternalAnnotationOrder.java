// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

import unavailable.library.Able;
import unavailable.library.Envelope;
import unavailable.library.Marker;
import unavailable.library.Tag;
import unavailable.library.Tags;
import unavailable.library.Zed;
import unavailable.otherWildcard.*;
import unavailable.wildcard.*;

class VisibleExternalAnnotationOrder {

    @Zed
    @Tag("longer")
    @Able
    @unavailable.library.Tag("a")
    int directRepeated;

    @Zed
    @Tags({@Tag("second"), @Tag("third")})
    @unavailable.library.Tag("first")
    @Able
    int explicitContainerFirst;

    @Zed
    @Tag("first")
    @unavailable.library.Tags({@unavailable.library.Tag("second"), @Tag("third")})
    @Able
    int explicitDirectFirst;

    @Zed
    @Tag("first")
    @Envelope({@Tags({@unavailable.library.Tag("second")})})
    @Able
    int nestedContainer;

    @Zed
    @Tag("first")
    @Envelope(
            tags = {@Tag("second")},
            markers = {@Marker("middle")})
    @Marker("last")
    @Able
    int transitiveOverlap;

    @Zed
    @Tag("first")
    @Marker("first")
    @Envelope(
            tags = {@Tag("second")},
            markers = {@Marker("second")})
    @Able
    int independentNestedTypes;

    @Zed
    @Able
    int independentTypes;

    @Zed
    @Tag("longer")
    @other.library.Tag("a")
    @Able
    int homonymousTypes;

    @Tags(label = "library-default")
    @Tag("direct")
    int omittedContainerValue;

    @Tags
    @unavailable.library.Tag("direct")
    int bareContainer;

    @Zed
    @WildcardTag("longer")
    @unavailable.wildcard.WildcardTag("a")
    @Able
    int ambiguousWildcard;
}
