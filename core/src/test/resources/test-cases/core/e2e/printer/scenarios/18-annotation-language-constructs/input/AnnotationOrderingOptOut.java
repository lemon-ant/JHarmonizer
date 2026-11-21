// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@Z @A
class AnnotationOrderingOptOut {

    // @jharmonizer:sort-off
    @Z
    // sort-off type header
    @A
    static class SortOff {

        @Z @A
        void execute(@Z @A String value) {}

        @Z
        // inherited sort-off type header
        @A
        static class NestedSortOff {

            @Z @A
            int value;

            @Z @A
            static class DeeplyNestedSortOff {

                @Z @A
                int value;
            }
        }
    }

    // @jharmonizer:fully-off
    @Z
    // fully-off type header
    @A
    static class FullyOff {

        @Z @A
        int value;

        @Z @A
        static class NestedFullyOff {

            @Z @A
            int value;

            @Z @A
            static class DeeplyNestedFullyOff {

                @Z @A
                int value;
            }
        }
    }

    @Z @A
    static class Enabled {

        @Z @A
        int value;

        @Z @A
        static class NestedEnabled {

            @Z @A
            int value;
        }
    }
}

@interface A {}

@interface Z {}
