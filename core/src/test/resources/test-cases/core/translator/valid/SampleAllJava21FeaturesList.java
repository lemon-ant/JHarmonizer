/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.translator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.concurrent.Callable;
import java.util.function.Function;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

// Annotation declarations
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface ClassAnnotation {
}

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface TypeAnnotation {
}

@interface FieldAnnotation {
}

@interface MethodAnnotation {
}

@interface ParameterAnnotation {
}

@interface ConstructorAnnotation {
}

@interface LocalVariableAnnotation {
}

/**
 * Ultimate Java Syntax Test File (Java 1.0 - 21)
 * Contains every valid Java syntax feature with minimal functionality
 * Designed for exhaustive translator testing
 */
@SuppressWarnings("PMD")
@SuppressFBWarnings
@ClassAnnotation
@Deprecated(forRemoval = true)
// IT WAS MOVED TO RESOURCES
public strictfp class SampleAllJava21FeaturesList<@TypeAnnotation T extends @TypeAnnotation Object> extends @TypeAnnotation
    Object implements @TypeAnnotation Serializable, Runnable, AutoCloseable {

    // ======== FIELD DECLARATIONS (ALL MODIFIERS) ========
    public static final int STATIC_FINAL_FIELD = 0b1010_1100; // Binary literal
    public static final String CONSTANT_REF = "REF_" + STATIC_FINAL_FIELD;

    // Static initializer
    static {
        System.out.println("Static initializer");
    }

    public int dependentField = STATIC_FINAL_FIELD * 2;
    volatile int volatileField;
    transient String transientField;
    // ======== JAVA 1.0-1.1 FEATURES ========
    // Basic types
    byte primitiveByte = 0x1A;
    short primitiveShort = 123;
    int primitiveInt = 1_234_567;
    long primitiveLong = 12345678901L;
    float primitiveFloat = 3.14f;
    double primitiveDouble = 2.71828;
    char primitiveChar = 'A';
    boolean primitiveBoolean = true;

    // Arrays
    int[] intArray = new int[10];
    String[][] stringMatrix = {{"a", "b"}, {"c", "d"}};
    // ======== JAVA 13 FEATURES ========
    // Text block with escape
    String textBlock =
        """
            Line 1: Text block
            Line 2: Special chars: \" \\ \t
            Line 3: \
            Concatenated""";
    // Anonymous class
    Callable<Integer> anonymous = new Callable<>() {
        @Override
        public Integer call() {
            return STATIC_FINAL_FIELD;
        }
    };
    @FieldAnnotation
    private List<@TypeAnnotation String> annotatedField;

    // Instance initializer
    {
        System.out.println("Instance initializer");
    }

    // ======== OTHER ESSENTIAL SYNTAX ========
    // Constructors
    public SampleAllJava21FeaturesList() {
    }

    @ConstructorAnnotation
    protected SampleAllJava21FeaturesList(int param) {
    }

    private SampleAllJava21FeaturesList(String... varargs) {
    }

    // Main method
    public static void main(String[] args) {
        var obj = new SampleAllJava21FeaturesList<>();
        System.out.println(obj.patternSwitch("Test"));
        obj.dependentMethod();
    }

    // ======== JAVA 5 FEATURES ========
    // Enhanced for-loop
    public void enhancedForLoop() {
        for (int num : intArray) {
            System.out.println(num);
        }
    }

    // Static import simulation
    void staticImportEquivalent() {
        double pi = Math.PI;
    }

    // ======== JAVA 7 FEATURES ========
    // Try-with-resources
    public void tryWithResources() throws IOException {
        try (InputStream is = new ByteArrayInputStream(new byte[10]);
             OutputStream os = new ByteArrayOutputStream()) {
            is.transferTo(os);
        }
    }

    // Multi-catch
    public void multiCatch() {
        try {
            Files.copy(Path.of("in.txt"), Path.of("out.txt"));
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
        }
    }

    // Try-with-resources enhancement
    public void tryWithResourcesFinal(InputStream input) throws IOException {
        final InputStream is = input;
        try (is) {
            System.out.println(is.read());
        }
    }

    // ======== JAVA 10 FEATURES ========
    // Local variable type inference
    public void varDeclaration() {
        var list = new ArrayList<String>();
        list.add("Java");
    }

    // ======== JAVA 11 FEATURES ========
    // Lambda parameter type inference
    public void lambdaVar() {
        Function<String, String> func = (var s) -> s.toUpperCase();
    }

    // ======== JAVA 12 FEATURES ========
    // Switch expression (preview)
    public String switchExpression(int value) {
        return switch (value) {
            case 1 -> "One";
            case 2 -> "Two";
            default -> "Many";
        };
    }

    // ======== JAVA 14 FEATURES ========
    // instanceof pattern matching
    public String patternMatching(Object obj) {
        if (obj instanceof String s) {
            return s.toUpperCase();
        }
        return "Not string";
    }

    // ======== JAVA 17 FEATURES ========
    // Pattern matching for switch (preview)
    public String patternSwitch(Object obj) {
        return switch (obj) {
            case Integer i -> "Integer: " + i;
            case String s when !s.isEmpty() -> "Non-empty String";
            case null, default -> "Other";
        };
    }

    // ======== JAVA 18 FEATURES ========
    // Simple web server (not syntax, skip)

    // ======== JAVA 19 FEATURES ========
    // Pattern matching switch enhancement
    public String nestedPattern(Object obj) {
        return switch (obj) {
            case Person(String n, int a) -> "Person: " + n;
            default -> "Unknown";
        };
    }

    // ======== JAVA 21 FEATURES ========
    // Record patterns
    public void recordPattern() {
        Person p = new Person("Alice", 30);
        if (p instanceof Person(String n, int a)) {
            System.out.println(n + ": " + a);
        }
    }

    // Pattern matching for switch
    public String exhaustiveSwitch(Shape s) {
        return switch (s) {
            case Circle c -> "Circle";
            case Square sq -> "Square";
        };
    }

    // Virtual threads
    public void virtualThread() {
        Thread.startVirtualThread(() -> {
            System.out.println("Virtual thread running");
        });
    }

    // SequencedCollection
    public SequencedCollection<String> sequencedCollection() {
        SequencedCollection<String> collection = new LinkedList<>();
        collection.addFirst("First");
        collection.addLast("Last");
        return collection;
    }

    // Methods
    @MethodAnnotation
    public synchronized <@TypeAnnotation U> U genericMethod(
        final @ParameterAnnotation U param, @ParameterAnnotation int... varargs)
        throws @TypeAnnotation IllegalArgumentException {
        // Local variables
        var localVar = List.of("var", "type");
        @LocalVariableAnnotation String typedVar = "annotated";

        // Field dependency
        int localCalculation = dependentField + STATIC_FINAL_FIELD;

        // Switch expression
        return (U)
            switch (param.toString().length()) {
                case 1, 2 -> "small";
                case 3 -> {
                    yield "medium";
                }
                default -> "large";
            };
    }

    // Assertions
    public void assertionTest() {
        assert STATIC_FINAL_FIELD > 0 : "Should be positive";
    }

    // Synchronized block
    public void synchronizedBlock() {
        synchronized (this) {
            System.out.println("Synchronized");
        }
    }

    @Override
    public void run() {
    }

    @Override
    public void close() {
    }

    // Dependency method
    public void dependentMethod() {
        int result = dependentField + STATIC_FINAL_FIELD;
        Runnable r = () -> System.out.println(result + " : " + CONSTANT_REF);
        r.run();
    }

    // ======== JAVA 8 FEATURES ========
    // Default method in interface
    public interface DefaultMethod {
        default void defaultMethod() {
            System.out.println("Default method");
        }
    }

    // Static method in interface
    public interface StaticMethod {
        static void staticMethod() {
            System.out.println("Static method");
        }
    }

    // ======== JAVA 9 FEATURES ========
    // Private interface method
    public interface PrivateMethod {
        private void privateMethod() {
            System.out.println("Private method");
        }
    }

    // ======== JAVA 15 FEATURES ========
    // Sealed classes
    public sealed interface Shape permits Circle, Square {
        double area();
    }

    public static non-sealed class Circle implements Shape {
        public double area() {
            return Math.PI * 10 * 10;
        }
    }

    public static final class Square implements Shape {
        public double area() {
            return 10 * 10;
        }
    }

    // ======== JAVA 16 FEATURES ========
    // Records
    public record Person(String name, int age) {
        public Person {
            Objects.requireNonNull(name);
        }
    }

    static class StaticNestedClass {
        enum NestedEnum {
            A,
            B,
            C
        }

        @interface NestedAnnotation {
            String value() default "";
        }
    }

    // Nested classes
    class InnerClass {
        private void innerMethod() {
            System.out.println(volatileField);
        }
    }
}
