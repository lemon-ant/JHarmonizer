package e2e;

public enum EnumScenario {
    BETA,
    ALPHA;

    void alpha() {}

    void beta() {}

    public static void main(String[] args) {
        if (values().length != 2 || values()[0] != BETA || values()[1] != ALPHA) {
            throw new IllegalStateException("Unexpected enum constants order");
        }
    }
}
