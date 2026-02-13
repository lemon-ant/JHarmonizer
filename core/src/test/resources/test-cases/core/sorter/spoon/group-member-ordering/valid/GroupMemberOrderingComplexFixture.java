package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

public class GroupMemberOrderingComplexFixture {

    private static final int y_provider = 2;
    private static final int w_provider = 1;
    private static final int z_provider = 4;
    private static final int x_provider = 3;

    private static final int a_dependent = w_provider + x_provider + y_provider + z_provider;
    private static final int a0_dependent2 = a_dependent + y_provider;
    private static final int b_dependent = a0_dependent2 + z_provider;
    private static final int b0_dependent2 = b_dependent + x_provider;

    private boolean enabledFlag;

    public void setEnabledFlag(boolean enabledFlag) {
        this.enabledFlag = enabledFlag;
    }

    public boolean isEnabledFlag() {
        return enabledFlag;
    }

    public boolean helloEnabledFlag() {
        return enabledFlag;
    }

    public Boolean getEnabledFlag() {
        return enabledFlag;
    }

    public boolean hasEnabledFlag() {
        return enabledFlag;
    }

    {
        int localValue = b0_dependent2;
        if (localValue > 0) {
            enabledFlag = true;
        }
    }
}
