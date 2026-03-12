package io.github.lemon_ant.jharmonizer.cli.logging;

import java.lang.reflect.Method;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

@Slf4j
@UtilityClass
@SuppressWarnings("PMD.GuardLogStatement")
public class LoggingConfigurator {

    public void setDebugLevel() {
        try {
            Object rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
            Object debugLevel = levelClass.getField("DEBUG").get(null);
            Method setLevel = rootLogger.getClass().getMethod("setLevel", levelClass);
            setLevel.invoke(rootLogger, debugLevel);
        } catch (ReflectiveOperationException e) {
            log.warn("Could not set DEBUG log level ({}): {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
