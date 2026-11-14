// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package annotationexternalcontainer;

import java.lang.annotation.Repeatable;

public class ExternalRepeatableContainerUse {

    @annotationexternalcontainer.library.Stamps
    @Stamp("a")
    int value;

    @Repeatable(annotationexternalcontainer.library.Stamps.class)
    public @interface Stamp {
        String value();
    }
}
