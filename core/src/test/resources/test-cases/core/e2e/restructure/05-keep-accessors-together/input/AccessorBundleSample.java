package e2e;

public class AccessorBundleSample {
    int value;

    String alpha() {
        return "alpha";
    }

    void setValue(int value) {
        this.value = value;
    }

    int getValue() {
        return value;
    }
}
