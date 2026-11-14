// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package annotationqualifiedshadow;

import lombok.*;
import lombok.EqualsAndHashCode.Exclude;

class ExternalAnnotationShadowConsumer {
    @EqualsAndHashCode.Exclude("longer")
    @ShadowAnnotationBase.Exclude("a")
    int value;
}

class ShadowAnnotationBase {
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Repeatable(Excludes.class)
    @interface Exclude {
        String value();
    }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @interface Excludes {
        Exclude[] value();
    }
}
