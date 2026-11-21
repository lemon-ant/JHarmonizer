// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package requiredparentlibrary;

import java.lang.annotation.Repeatable;

public class RequiredExternalAnnotationBase {
    @Repeatable(Tags.class)
    public @interface Tag {}

    public @interface Tags {
        Tag[] value();
    }
}
