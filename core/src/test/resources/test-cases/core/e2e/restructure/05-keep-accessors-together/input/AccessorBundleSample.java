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

    public static void main(String[] args) {
        AccessorBundleSample sample = new AccessorBundleSample();
        sample.setValue(7);
        if (sample.getValue() != 7) {
            throw new IllegalStateException("Unexpected accessor value: " + sample.getValue());
        }
    }
}
