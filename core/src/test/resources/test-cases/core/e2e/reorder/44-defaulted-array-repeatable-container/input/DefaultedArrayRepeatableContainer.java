// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package annotationdefaultedarraycontainer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;

public class DefaultedArrayRepeatableContainer {

    // This group is collected before the later default annotation can expose the repeated type's metadata.
    String @Zed @Owner.Tags @annotationdefaultedarraycontainer.DefaultedArrayRepeatableContainer.Owner.Tag("direct") @Able [] values;

    public static void main(String[] arguments) throws ReflectiveOperationException {
        List<String> actualValues = Arrays.stream(DefaultedArrayRepeatableContainer.class
                        .getDeclaredField("values")
                        .getAnnotatedType()
                        .getAnnotationsByType(Owner.Tag.class))
                .map(Owner.Tag::value)
                .toList();
        if (!actualValues.equals(List.of("default", "direct"))) {
            throw new IllegalStateException("Unexpected repeatable annotation order: " + actualValues);
        }
    }

    static class Owner {

        @Repeatable(Tags.class)
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.TYPE_USE)
        @interface Tag {
            String value();
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.TYPE_USE)
        @interface Tags {
            Tag[] value() default {@Tag("default")};
        }
    }

    @Target(ElementType.TYPE_USE)
    @interface Able {}

    @Target(ElementType.TYPE_USE)
    @interface Zed {}
}
