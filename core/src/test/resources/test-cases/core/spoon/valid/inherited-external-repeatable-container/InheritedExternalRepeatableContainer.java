// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package annotationinheritedexternalcontainer;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class InheritedExternalRepeatableContainer {

    @Owner.Tags
    @Tag("a")
    int value;

    @Repeatable(Derived.Tags.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Tag {
        String value();
    }

    public static class Derived extends annotationinheritedexternalcontainer.dependencies.HiddenBase {}

    public static class Owner {

        @Retention(RetentionPolicy.RUNTIME)
        public @interface Tags {
            Tag[] value() default {@Tag("z")};
        }
    }
}
