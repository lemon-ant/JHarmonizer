// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
import java.util.List;

/** Source header belongs to the main type. */
public class TypeDeclarationsScenario {
    private List<String> zebra;
    private List<String> alpha;

    void zebra() { }
    void alpha() { }

    @interface NestedAnnotation {
        String zebra();
        String alpha();
    }

    interface NestedInterface {
        void zebra();
        void alpha();
    }

    record NestedRecord(int value) {
        int zebra() { return value; }
        int alpha() { return value; }
    }

    enum NestedEnum {
        SECOND, FIRST;
        private static final String LABEL = "two  spaces";
        void zebra() { }
        void alpha() { }
    }

    static class EmptyNested { /* Keep the body comment. */ }
}

class EmptySibling { /* Keep the sibling comment. */ }
