package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

public class GroupMemberOrderingComplexFixture {

    private int x_provider = 10;

    private int y_provider = x_provider + 1;

    private int z_provider = y_provider + 1;

    private int w_provider = 5;

    private final int z_blank_final;

    {
        z_blank_final = w_provider + x_provider + y_provider + z_provider;
    }

    private int a_dependent = z_blank_final + w_provider + x_provider + y_provider + z_provider;

    private int a0_dependent2 = a_dependent + 1;

    private boolean enabledFlag;

    public boolean isEnabledFlag() {
        return enabledFlag;
    }

    public boolean hasEnabledFlag() {
        return enabledFlag;
    }

    public Boolean getEnabledFlag() {
        return enabledFlag;
    }

    public void setEnabledFlag(boolean enabledFlag) {
        this.enabledFlag = enabledFlag;
    }
}
