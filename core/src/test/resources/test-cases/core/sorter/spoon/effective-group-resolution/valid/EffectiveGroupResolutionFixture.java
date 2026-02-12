package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

public class EffectiveGroupResolutionFixture {

    public static final int PROVIDER = 1;
    public static final int DIRECT_DEPENDENT = PROVIDER + 1;
    public static final int TRANSITIVE_PROVIDER = PROVIDER + 10;
    public static final int TRANSITIVE_DEPENDENT = TRANSITIVE_PROVIDER + 1;
    public static final int UNRELATED = 5;

    static {
        int ignoredValue = PROVIDER;
    }
}
