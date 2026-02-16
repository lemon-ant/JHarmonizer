package e2e;

public class StaticInstanceInitializerSample {
    static {
        int staticValue = 2;
    }

    {
        int instance = 1;
    }
}
