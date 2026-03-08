package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class GroupOrderingRuleFixture {

    public int publicFieldFirst = 1;
    public int publicFieldSecond = 2;

    protected int protectedField = 3;

    int packagePrivateField = 4;

    private int privateField = 5;

    private int zuluField = 0;
    private int alphaField = 0;
    private int bravoField = 0;

    public int calculate() {
        return 0;
    }

    public int calculate(int number) {
        return number;
    }

    public int calculate(String text) {
        return text.length();
    }

    public int getValue() {
        return privateField;
    }

    public void middleMethod() {}

    public void setValue(int value) {
        this.privateField = value;
    }

    private GroupOrderingRuleFixture() {}
}
