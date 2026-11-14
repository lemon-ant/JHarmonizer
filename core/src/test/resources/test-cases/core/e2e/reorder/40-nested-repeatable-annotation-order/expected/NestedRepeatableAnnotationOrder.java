// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class NestedRepeatableAnnotationOrder {

    @Able
    @Tags({@Tag("z"), @Tag("y")})
    @Tags({@Tag("a")})
    @Zed
    int repeatedContainers;

    @Able
    @Tags({@Tag("first")})
    @TagGroups({@Tags({@Tag("second"), @Tag("third")})})
    @Zed
    int explicitOuterContainerLast;

    @Able
    @Tags({@Tag("direct")})
    @TagGroups
    @Zed
    int defaultOuterContainerLast;

    @Able
    @TagGroups
    @Tag("direct")
    @Zed
    int defaultOuterContainerBeforeLeaf;

    public static void main(String[] arguments) throws ReflectiveOperationException {
        Map<String, List<List<String>>> expectedGroupsByField = Map.of(
                "repeatedContainers", List.of(List.of("z", "y"), List.of("a")),
                "explicitOuterContainerLast", List.of(List.of("first"), List.of("second", "third")),
                "defaultOuterContainerLast", List.of(List.of("direct"), List.of("default")),
                "defaultOuterContainerBeforeLeaf", List.of(List.of("default")));
        for (Map.Entry<String, List<List<String>>> expectedField : expectedGroupsByField.entrySet()) {
            List<List<String>> actualGroups = Arrays.stream(NestedRepeatableAnnotationOrder.class
                            .getDeclaredField(expectedField.getKey())
                            .getAnnotationsByType(Tags.class))
                    .map(container -> Arrays.stream(container.value()).map(Tag::value).toList())
                    .toList();
            if (!actualGroups.equals(expectedField.getValue())) {
                throw new IllegalStateException(expectedField.getKey() + ": expected " + expectedField.getValue()
                        + ", actual " + actualGroups);
            }
        }
    }

    @interface Able {}

    @interface Zed {}

    @Repeatable(Tags.class)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Tag {
        String value();
    }

    @Repeatable(TagGroups.class)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Tags {
        Tag[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface TagGroups {
        Tags[] value() default {@Tags({@Tag("default")})};
    }
}
