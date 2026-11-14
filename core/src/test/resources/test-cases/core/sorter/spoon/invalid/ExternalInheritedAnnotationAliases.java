// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

import static unavailable.StaticDerived.Mark;

import java.util.List;
import unavailable.Base.Tag;
import unavailable.Derived;
import unavailable.library.Able;
import unavailable.library.Zed;

class ExternalInheritedAnnotationAliases {

    @Zed
    @Tag("z")
    @unavailable.Derived.Tag("a")
    @Able
    int typeImportOwnerAlias;

    @Zed
    @Derived.Tag("z")
    @unavailable.Base.Tag("a")
    @Able
    int importedOwnerAlias;

    @Zed
    @Mark("z")
    @unavailable.StaticBase.Mark("a")
    @Able
    int staticImportOwnerAlias;

    @Zed
    @unavailable.Derived.Tags({@unavailable.Derived.Tag("z")})
    @Tag("a")
    @Able
    int directAndNestedOwnerAlias;

    @Zed
    @unavailable.Derived.Tags({@unavailable.Derived.Tag("z")})
    @unavailable.Base.Tags({@Tag("a")})
    @Able
    int nestedOwnerAliases;

    @Zed
    @unavailable.Base.Tag("z")
    @unavailable.Derived.Tag("a")
    @Able
    int qualifiedOwnerAliases;
}
