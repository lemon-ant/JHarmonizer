package io.github.lemon_ant.jharmonizer.core.e2e;

public class SeparatorScenario {
    // Header fields
    int a = 1;
    int z = 2;

    void alpha() {}

    void gamma() {}

    public static void main(String[] args) {
        SeparatorScenario sample = new SeparatorScenario();
        if (sample.a != 1 || sample.z != 2) {
            throw new IllegalStateException("Unexpected separator scenario fields");
        }
    }
}
