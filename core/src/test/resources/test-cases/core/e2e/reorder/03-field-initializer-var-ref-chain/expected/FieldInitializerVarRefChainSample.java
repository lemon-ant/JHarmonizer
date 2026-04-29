package io.github.lemon_ant.jharmonizer.core.e2e;

public class FieldInitializerVarRefChainSample {
    int a = 12; // literal 12; independent constant field
    int h = 1; // literal 1; anchor for the h -> d -> b dependency chain
    int d = h + 3; // expects 4; depends on h, so h must stay before d
    int b = d + 9; // expects 13; depends on d, so d must stay before b
    int c = 5; // literal 5; independent constant field
    int e = 15; // literal 15; independent constant field
    int g = 25; // literal 25; independent constant field
    int f = 21 + g; // expects 46; depends on g, so g must stay before f
    int i = g + 27; // expects 52; depends on g, so g must stay before i

    public static void main(String[] args) {
        FieldInitializerVarRefChainSample sample = new FieldInitializerVarRefChainSample();
        if (sample.a != 12
                || sample.b != 13
                || sample.c != 5
                || sample.d != 4
                || sample.e != 15
                || sample.f != 46
                || sample.g != 25
                || sample.h != 1
                || sample.i != 52) {
            throw new IllegalStateException("Unexpected field values after initialization:"
                    + " a="
                    + sample.a
                    + ", b="
                    + sample.b
                    + ", c="
                    + sample.c
                    + ", d="
                    + sample.d
                    + ", e="
                    + sample.e
                    + ", f="
                    + sample.f
                    + ", g="
                    + sample.g
                    + ", h="
                    + sample.h
                    + ", i="
                    + sample.i);
        }
    }
}
