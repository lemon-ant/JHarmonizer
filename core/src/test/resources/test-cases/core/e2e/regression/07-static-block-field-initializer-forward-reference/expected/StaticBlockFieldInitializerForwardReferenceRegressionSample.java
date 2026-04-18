package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StaticBlockFieldInitializerForwardReferenceRegressionSample {
    private static final Map<Integer, String> ITEM_REGISTRY = new HashMap<>();

    static {
        ITEM_REGISTRY.put(1, "alpha");
        ITEM_REGISTRY.put(2, "beta");
        ITEM_REGISTRY.put(3, "gamma");
    }

    static final List<String> REGISTRY_SNAPSHOT = List.copyOf(ITEM_REGISTRY.values());

    public static void main(String[] args) {
        if (REGISTRY_SNAPSHOT.size() != 3
                || !REGISTRY_SNAPSHOT.contains("alpha")
                || !REGISTRY_SNAPSHOT.contains("beta")
                || !REGISTRY_SNAPSHOT.contains("gamma")) {
            throw new IllegalStateException(
                    "REGISTRY_SNAPSHOT must contain alpha, beta, gamma but was: " + REGISTRY_SNAPSHOT);
        }
    }
}
