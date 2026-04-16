package io.github.lemon_ant.jharmonizer.core.e2e;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class InstanceBlockStaticFieldMutationRegressionSample {
    private static final Map<Integer, String> ITEM_REGISTRY = new HashMap<>();

    {
        ITEM_REGISTRY.put(1, "alpha");
        ITEM_REGISTRY.put(2, "beta");
        ITEM_REGISTRY.put(3, "gamma");
    }

    private final List<String> registrySnapshot = List.copyOf(ITEM_REGISTRY.values());

    public static void main(String[] args) {
        InstanceBlockStaticFieldMutationRegressionSample instance =
                new InstanceBlockStaticFieldMutationRegressionSample();
        if (instance.registrySnapshot.size() != 3
                || !instance.registrySnapshot.contains("alpha")
                || !instance.registrySnapshot.contains("beta")
                || !instance.registrySnapshot.contains("gamma")) {
            throw new IllegalStateException(
                    "registrySnapshot must contain alpha, beta, gamma but was: " + instance.registrySnapshot);
        }
    }
}
