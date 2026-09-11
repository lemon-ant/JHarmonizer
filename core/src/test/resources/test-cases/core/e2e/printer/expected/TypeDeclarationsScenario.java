// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
import java.util.List;

/** Source header belongs to the main type. */
public class TypeDeclarationsScenario {
    private List<String> alpha;
    private List<String> zebra;

    void alpha() { }

    void zebra() { }


    @interface NestedAnnotation {
        String alpha();

        String zebra();

    }


    interface NestedInterface {
        void alpha();

        void zebra();

    }


    static class EmptyNested { /* Keep the body comment. */ }



    enum NestedEnum {
        SECOND, FIRST;

        private static final String LABEL = "two  spaces";

        void alpha() { }

        void zebra() { }

    }


    record NestedRecord(int value) {
        int alpha() { return value; }

        int zebra() { return value; }

    }

}

class EmptySibling { /* Keep the sibling comment. */ }

