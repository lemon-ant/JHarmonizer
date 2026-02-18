package e2e;

public record RecordScenario(int value) {
    String zeta() {
        return "z";
    }

    String alpha() {
        return "a";
    }

    public static void main(String[] args) {
        RecordScenario sample = new RecordScenario(5);
        if (sample.value() != 5 || !"a".equals(sample.alpha()) || !"z".equals(sample.zeta())) {
            throw new IllegalStateException("Unexpected record behavior");
        }
    }
}
