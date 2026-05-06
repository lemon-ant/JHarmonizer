// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
enum ZebraKind {
    ONE
}

@interface Marker {}

interface AlphaContract {}

record BravoRecord(int value) {}

class AlphaHelper {
    static String message() {
        return "ok";
    }
}

public class MainTypeFirstFixture {
    public static void main(String[] args) {
        System.out.println(new BravoRecord(1).value()
                + AlphaHelper.message()
                + AlphaContract.class.getSimpleName()
                + ZebraKind.ONE.name()
                + Marker.class.getSimpleName());
    }
}
