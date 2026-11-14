// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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

    @DefaultTags
    @DefaultTag("direct")
    int defaultContainerFirst;

    @DefaultTags(label = "named")
    @DefaultTag("direct")
    int namedDefaultContainerFirst;

    @DefaultTag("direct")
    @DefaultTags
    int defaultContainerLast;

    @Right.Tag("x")
    @Left.Tag("longer")
    int differentTypes;

    @Tag("longer")
    @Tag("z")
    @Note("b")
    int competingArguments;

    @Tag("longer")
    @Tag("aa")
    @Note("a")
    int tiedDeclarations;

    String @TypeTag("longer") @RepeatableAnnotationRuntimeOrder.TypeTag("z") [] typeUseQualifiedArray;
    String @TypeTags({@TypeTag("second"), @TypeTag("third")}) @TypeTag("first") [] typeUseContainerArray;

    public static void main(String[] arguments) throws ReflectiveOperationException {
        Map<String, List<String>> expectedValuesByField = Map.of(
                "declarationLengths", List.of("longer", "z"),
                "arguments", List.of("z", "a"),
                "separatedOccurrences", List.of("first", "x"),
                "qualifiedOccurrences", List.of("first", "x"),
                "explicitContainerFirst", List.of("second", "third", "first"),
                "explicitContainerLast", List.of("first", "second", "third"),
                "competingArguments", List.of("longer", "z"),
                "tiedDeclarations", List.of("longer", "aa"));
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
        verifyTypeTagOrder(
                RepeatableAnnotationRuntimeOrder.class.getDeclaredField("typeUseQualifiedArray")
                        .getAnnotatedType().getAnnotationsByType(TypeTag.class),
                "longer", "z");
        verifyTypeTagOrder(
                RepeatableAnnotationRuntimeOrder.class.getDeclaredField("typeUseContainerArray")
                        .getAnnotatedType().getAnnotationsByType(TypeTag.class),
                "second", "third", "first");
        verifyTypeTagOrder(
                RepeatableAnnotationRuntimeOrder.class.getDeclaredMethod("receive")
                        .getAnnotatedReceiverType().getAnnotationsByType(TypeTag.class),
                "first", "x");
        verifyDefaultTagOrder("defaultContainerFirst", "default", "direct");
        verifyDefaultTagOrder("namedDefaultContainerFirst", "default", "direct");
        verifyDefaultTagOrder("defaultContainerLast", "direct", "default");
    }

    void receive(@RepeatableAnnotationRuntimeOrder.TypeTag("first") @TypeTag("x") RepeatableAnnotationRuntimeOrder this) {}

    private static void verifyTypeTagOrder(TypeTag[] annotations, String... expectedValues) {
        List<String> actualValues = Arrays.stream(annotations).map(TypeTag::value).toList();
        if (!actualValues.equals(List.of(expectedValues))) {
            throw new IllegalStateException("expected " + List.of(expectedValues) + ", actual " + actualValues);
        }
    }

    private static void verifyDefaultTagOrder(String fieldName, String... expectedValues)
            throws ReflectiveOperationException {
        List<String> actualValues = Arrays.stream(RepeatableAnnotationRuntimeOrder.class
                        .getDeclaredField(fieldName)
                        .getAnnotationsByType(DefaultTag.class))
                .map(DefaultTag::value)
                .toList();
        if (!actualValues.equals(List.of(expectedValues))) {
            throw new IllegalStateException(
                    fieldName + ": expected " + List.of(expectedValues) + ", actual " + actualValues);
        }
    }

    @Repeatable(DefaultTags.class)
    @Retention(RetentionPolicy.RUNTIME)
    @interface DefaultTag {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface DefaultTags {
        DefaultTag[] value() default {@DefaultTag("default")};

        String label() default "x";
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

    @interface Note {
        String value();
    }

    @Repeatable(TypeTags.class)
    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface TypeTag {
        String value();
    }

    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface TypeTags {
        TypeTag[] value();
    }

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
