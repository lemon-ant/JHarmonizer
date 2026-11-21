// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package annotationimportedexternalcontainer;

import annotationimportedexternalcontainer.library.Stamps;
import java.lang.annotation.Repeatable;

public class ImportedExternalRepeatableContainer {

    @Stamp("a")
    @Stamps
    int value;

    @Repeatable(Stamps.class)
    public @interface Stamp {
        String value();
    }
}
