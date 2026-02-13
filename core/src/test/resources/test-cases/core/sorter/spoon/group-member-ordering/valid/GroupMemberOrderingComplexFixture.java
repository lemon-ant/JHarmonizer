package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

public class GroupMemberOrderingComplexFixture {

    private static final int y_provider = 2;
    private static final int w_provider = 1;
    private static final int z_provider = 4;
    private static final int x_provider = 3;

    private static final int a_dependent = w_provider + x_provider + y_provider + z_provider;
    private static final int c_dependent = a_dependent + y_provider;

    private boolean enabledFlag;
    {
        int localValue = c_dependent;
        if (localValue > 0) {
            enabledFlag = true;
        }
    }

    private static final int b_dependent = c_dependent + z_provider;
    private static final int d_dependent = b_dependent + x_provider;

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
}
