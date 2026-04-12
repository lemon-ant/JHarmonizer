package io.github.lemon_ant.jharmonizer.core.e2e;

public class PrinterConfigBlankLineBetweenFieldsEnabledScenario {
    int alpha = 1;

    @Deprecated
    int beta = 2;

    int gamma = 3;

    void alpha() {}

    void beta() {}

    public static void main(String[] args) {
        PrinterConfigBlankLineBetweenFieldsEnabledScenario instance =
                new PrinterConfigBlankLineBetweenFieldsEnabledScenario();
        if (instance.alpha != 1 || instance.beta != 2 || instance.gamma != 3) {
            throw new IllegalStateException("Unexpected field values");
        }
    }
}
