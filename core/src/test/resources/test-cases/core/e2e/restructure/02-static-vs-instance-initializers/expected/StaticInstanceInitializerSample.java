package e2e;

public class StaticInstanceInitializerSample {
    {
        int instance = 1;
    }

    static {
        int staticValue = 2;
    }

}
