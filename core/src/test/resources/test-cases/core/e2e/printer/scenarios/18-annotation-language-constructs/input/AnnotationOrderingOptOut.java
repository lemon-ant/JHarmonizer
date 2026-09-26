// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

@Z @A
class AnnotationOrderingOptOut {

    // @jharmonizer:sort-off
    @Z @A
    static class SortOff {

        @Z @A
        void execute(@Z @A String value) {}
    }

    // @jharmonizer:fully-off
    @Z @A
    static class FullyOff {

        @Z @A
        int value;
    }

    @Z @A
    static class Enabled {

        @Z @A
        int value;
    }
}

@interface A {}

@interface Z {}
