package io.github.lemon_ant.jharmonizer.cli.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import lombok.experimental.UtilityClass;
import org.slf4j.LoggerFactory;

@UtilityClass
public class LoggingConfigurator {

    public void setDebugLevel() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.DEBUG);
    }
}
