package io.github.lemon_ant.jharmonizer.core.e2e;

class FourTypeCrossTypeChainBackRefRegressionSample {
    static final String FIRST_LABEL = new String("First");
    static final String SECOND_LABEL = new String("Second");
    static final String COMBINED_VALUE = FourTypeCrossTypeChainLink1.VALUE;

    public static void main(String[] args) {
        if (!"Second/via-3/via-2/via-1".equals(COMBINED_VALUE)) {
            throw new IllegalStateException("Expected 'Second/via-3/via-2/via-1' but was: " + COMBINED_VALUE);
        }
    }
}

class FourTypeCrossTypeChainLink1 {
    static final String VALUE = new String(FourTypeCrossTypeChainLink2.VALUE + "/via-1");
}

class FourTypeCrossTypeChainLink2 {
    static final String VALUE = new String(FourTypeCrossTypeChainLink3.VALUE + "/via-2");
}

class FourTypeCrossTypeChainLink3 {
    static final String VALUE = new String(FourTypeCrossTypeChainBackRefRegressionSample.SECOND_LABEL + "/via-3");
}
