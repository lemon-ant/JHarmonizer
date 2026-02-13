package e2e;

public class AccessorBundleSample {
    int value;

    String alpha() {
        return "alpha";
    }

    int getValue() {
        return value;
    }

    void setValue(int value) {
        this.value = value;
    }
}
