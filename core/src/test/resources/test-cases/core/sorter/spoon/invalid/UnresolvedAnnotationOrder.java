// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

class UnresolvedAnnotationOrder {

    @Zed
    @Able
    int independentShortNames;

    @Zed
    @external.Tags({@Tag("second")})
    @external.Tag("first")
    @Able
    int unresolvedContainerValue;

    @external.Zed
    @Tag("first")
    @other.Able
    int mixedNames;

    @Zed
    @Tag("longer")
    @Able
    @Tag("a")
    int repeatedShortNames;
}
