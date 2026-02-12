package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

public class EffectiveGroupResolutionBlankFinalFixture {

    private static final int BLANK_FINAL;

    static {
        BLANK_FINAL = 42;
    }

    public static final int READS_BLANK_FINAL = BLANK_FINAL + 1;
    public static final int UNRELATED = 7;
}
