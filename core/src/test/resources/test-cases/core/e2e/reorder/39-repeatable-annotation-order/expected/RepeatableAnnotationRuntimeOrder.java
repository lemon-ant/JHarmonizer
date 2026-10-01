// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class RepeatableAnnotationRuntimeOrder {

    @Tag("longer")
    @Tag("z")
    int declarationLengths;

    @Tag("z")
    @Tag("a")
    int arguments;

    @Marker
    @Tag("first")
    @Tag("x")
    int separatedOccurrences;

    @RepeatableAnnotationRuntimeOrder.Tag("first")
    @Tag("x")
    int qualifiedOccurrences;

    @Tags({@Tag("second"), @Tag("third")})
    @Tag("first")
    int explicitContainerFirst;

    @Tag("first")
    @Tags({@Tag("second"), @Tag("third")})
    int explicitContainerLast;

    @Right.Tag("x")
    @Left.Tag("longer")
    int differentTypes;

    public static void main(String[] arguments) throws ReflectiveOperationException {
        Map<String, List<String>> expectedValuesByField = Map.of(
                "declarationLengths", List.of("longer", "z"),
                "arguments", List.of("z", "a"),
                "separatedOccurrences", List.of("first", "x"),
                "qualifiedOccurrences", List.of("first", "x"),
                "explicitContainerFirst", List.of("second", "third", "first"),
                "explicitContainerLast", List.of("first", "second", "third"));
        List<String> orderViolations = new ArrayList<>();
        for (Map.Entry<String, List<String>> expectedField : expectedValuesByField.entrySet()) {
            List<String> actualValues = Arrays.stream(RepeatableAnnotationRuntimeOrder.class
                            .getDeclaredField(expectedField.getKey())
                            .getAnnotationsByType(Tag.class))
                    .map(Tag::value)
                    .toList();
            if (!actualValues.equals(expectedField.getValue())) {
                orderViolations.add(expectedField.getKey() + ": expected " + expectedField.getValue()
                        + ", actual " + actualValues);
            }
        }
        if (!orderViolations.isEmpty()) {
            throw new IllegalStateException(String.join(System.lineSeparator(), orderViolations));
        }
    }

    @Repeatable(Tags.class)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Tag {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Tags {
        Tag[] value();
    }

    @interface Marker {}

    static class Left {

        @interface Tag {
            String value();
        }
    }

    static class Right {

        @interface Tag {
            String value();
        }
    }
}
