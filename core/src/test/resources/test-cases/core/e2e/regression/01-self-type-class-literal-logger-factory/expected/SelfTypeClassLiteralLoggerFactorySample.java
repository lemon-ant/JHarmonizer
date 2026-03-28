package io.github.lemon_ant.jharmonizer.core.e2e;

public class SelfTypeClassLiteralLoggerFactorySample {
    private static final Object logger = SelfLoggerFactory.resolve(SelfTypeClassLiteralLoggerFactorySample.class);

    private static final String message = "ok";

    public static void main(String[] args) {
        if (!message.equals("ok") || !logger.toString().contains("SelfTypeClassLiteralLoggerFactorySample")) {
            throw new IllegalStateException("Unexpected logger resolution outcome: " + logger);
        }
    }

    private static final class SelfLoggerFactory {
        private static Object resolve(Class<?> ownerType) {
            return ownerType.getName();
        }

        private SelfLoggerFactory() {
            // utility
        }
    }
}
