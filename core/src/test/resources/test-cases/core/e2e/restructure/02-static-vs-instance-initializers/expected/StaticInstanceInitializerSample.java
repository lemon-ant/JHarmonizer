package e2e;

public class StaticInstanceInitializerSample {
    private static String staticOrder = "";
    private String instanceOrder = "";

    static {
        staticOrder += "S1";
    }

    {
        instanceOrder += "I1";
    }

    public static void main(String[] args) {
        StaticInstanceInitializerSample sample = new StaticInstanceInitializerSample();
        if (!"S1".equals(staticOrder)) {
            throw new IllegalStateException("Unexpected static initializer order: " + staticOrder);
        }
        if (!"I1".equals(sample.instanceOrder)) {
            throw new IllegalStateException("Unexpected instance initializer order: " + sample.instanceOrder);
        }
    }
}
