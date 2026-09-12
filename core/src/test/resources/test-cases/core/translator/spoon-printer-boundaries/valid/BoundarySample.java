// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.printerboundaries;

class BoundarySample {


    // declared members
    @Deprecated
    int first = 1; // inline note

    int second = 2;

    // nested type comment
    @Deprecated
    static class Nested {


        // first member comment
        @Deprecated
        int value = 3;


    }

    // @jharmonizer:fully-off
    static class Preserved {
      int z;     int a;


      void keep( ) { }
    }



}

@interface Empty {}
