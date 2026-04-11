package io.github.lemon_ant.jharmonizer.core.e2e;

public class PrinterConfigBlankLineAfterTypeHeaderEnabledScenario {

    int alpha = 1;
    int beta = 2;

    void alpha() {}

    void beta() {}

    public static void main(String[] args) {
        PrinterConfigBlankLineAfterTypeHeaderEnabledScenario instance =
                new PrinterConfigBlankLineAfterTypeHeaderEnabledScenario();
        if (instance.alpha != 1 || instance.beta != 2) {
            throw new IllegalStateException("Unexpected field values");
        }
        if (Status.values().length != 2) {
            throw new IllegalStateException("Unexpected enum size");
        }
        Nested nested = new Nested();
        if (nested.alpha != 1 || nested.beta != 2) {
            throw new IllegalStateException("Unexpected nested field values");
        }
    }

    enum Status {
        ACTIVE,
        INACTIVE;

        void alpha() {}

        void beta() {}
    }

    static class Nested {

        int alpha = 1;
        int beta = 2;

        void alpha() {}

        void beta() {}
    }
}
