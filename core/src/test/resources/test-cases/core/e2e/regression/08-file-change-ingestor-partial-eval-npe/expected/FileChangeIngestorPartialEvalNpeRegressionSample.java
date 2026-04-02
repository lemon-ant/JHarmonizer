package org.apache.nifi.minifi.bootstrap.configuration.ingestors;

import static java.nio.ByteBuffer.wrap;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Predicate.not;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.nifi.minifi.bootstrap.configuration.ConfigurationChangeCoordinator.NOTIFIER_INGESTORS_KEY;
import static org.apache.nifi.minifi.bootstrap.configuration.differentiators.WholeConfigDifferentiator.WHOLE_CONFIG_KEY;
import static org.apache.nifi.minifi.commons.api.MiNiFiConstants.RAW_EXTENSION;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.apache.nifi.minifi.bootstrap.ConfigurationFileHolder;
import org.apache.nifi.minifi.bootstrap.configuration.ConfigurationChangeNotifier;
import org.apache.nifi.minifi.bootstrap.configuration.differentiators.Differentiator;
import org.apache.nifi.minifi.bootstrap.configuration.differentiators.WholeConfigDifferentiator;
import org.apache.nifi.minifi.bootstrap.configuration.ingestors.interfaces.ChangeIngestor;
import org.apache.nifi.minifi.commons.api.MiNiFiProperties;
import org.apache.nifi.minifi.properties.BootstrapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileChangeIngestorPartialEvalNpeRegressionSample implements Runnable, ChangeIngestor {
    private static final Logger logger =
            LoggerFactory.getLogger(FileChangeIngestorPartialEvalNpeRegressionSample.class);

    static final String CONFIG_FILE_BASE_KEY = NOTIFIER_INGESTORS_KEY + ".file";
    static final String CONFIG_FILE_PATH_KEY = CONFIG_FILE_BASE_KEY + ".config.path";
    static final int DEFAULT_POLLING_PERIOD_INTERVAL = 15;
    static final String POLLING_PERIOD_INTERVAL_KEY = CONFIG_FILE_BASE_KEY + ".polling.period.seconds";
    private static final TimeUnit DEFAULT_POLLING_PERIOD_UNIT = SECONDS;
    private static final Map<String, Supplier<Differentiator<ByteBuffer>>> DIFFERENTIATOR_CONSTRUCTOR_MAP =
            Map.of(WHOLE_CONFIG_KEY, WholeConfigDifferentiator::getByteBufferDifferentiator);
    private static final String DIFFERENTIATOR_KEY = CONFIG_FILE_BASE_KEY + ".differentiator";

    private Path configFilePath;
    private volatile ConfigurationChangeNotifier configurationChangeNotifier;
    private volatile Differentiator<ByteBuffer> differentiator;
    private ScheduledExecutorService executorService;
    private long pollingSeconds;
    private WatchService watchService;

    @Override
    public void close() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Override
    public void initialize(
            BootstrapProperties properties,
            ConfigurationFileHolder configurationFileHolder,
            ConfigurationChangeNotifier configurationChangeNotifier) {
        Path configFile = ofNullable(properties.getProperty(CONFIG_FILE_PATH_KEY))
                .filter(not(String::isBlank))
                .map(Path::of)
                .map(Path::toAbsolutePath)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Property, " + CONFIG_FILE_PATH_KEY + ", for the path of the config file must be specified"));
        try {
            this.configurationChangeNotifier = configurationChangeNotifier;
            this.configFilePath = configFile;
            this.pollingSeconds = ofNullable(properties.getProperty(
                            POLLING_PERIOD_INTERVAL_KEY, Long.toString(DEFAULT_POLLING_PERIOD_INTERVAL)))
                    .map(Long::parseLong)
                    .filter(duration -> duration > 0)
                    .map(duration -> SECONDS.convert(duration, DEFAULT_POLLING_PERIOD_UNIT))
                    .orElseThrow(
                            () -> new IllegalArgumentException("Cannot specify a polling period with duration <=0"));
            this.watchService = initializeWatcher(configFile);
            this.differentiator = ofNullable(properties.getProperty(DIFFERENTIATOR_KEY))
                    .filter(not(String::isBlank))
                    .map(differentiator -> ofNullable(DIFFERENTIATOR_CONSTRUCTOR_MAP.get(differentiator))
                            .map(Supplier::get)
                            .orElseThrow(unableToFindDifferentiatorExceptionSupplier(differentiator)))
                    .orElseGet(WholeConfigDifferentiator::getByteBufferDifferentiator);
            this.differentiator.initialize(configurationFileHolder);
        } catch (Exception e) {
            throw new IllegalStateException("Could not successfully initialize file change notifier", e);
        }

        checkConfigFileLocationCorrectness(properties, configFile);
    }

    @Override
    public void run() {
        logger.debug("Checking for a change in {}", configFilePath);
        if (targetFileChanged()) {
            logger.debug("Target file changed, checking if it's different than current flow");
            try (FileInputStream flowCandidateInputStream = new FileInputStream(configFilePath.toFile())) {
                ByteBuffer newFlowConfig = wrap(toByteArray(flowCandidateInputStream));
                if (differentiator.isNew(newFlowConfig)) {
                    logger.debug("Current flow and new flow is different, notifying listener");
                    configurationChangeNotifier.notifyListeners(newFlowConfig);
                    logger.debug("Listeners have been notified");
                }
            } catch (Exception e) {
                logger.error("Could not successfully notify listeners.", e);
            }
        } else {
            logger.debug("No change detected in {}", configFilePath);
        }
    }

    @Override
    public void start() {
        executorService = Executors.newScheduledThreadPool(1, runnable -> {
            Thread notifierThread = Executors.defaultThreadFactory().newThread(runnable);
            notifierThread.setName("File Change Notifier Thread");
            notifierThread.setDaemon(true);
            return notifierThread;
        });
        executorService.scheduleWithFixedDelay(this, 0, pollingSeconds, DEFAULT_POLLING_PERIOD_UNIT);
    }

    void setConfigFilePath(Path configFilePath) {
        this.configFilePath = configFilePath;
    }

    void setConfigurationChangeNotifier(ConfigurationChangeNotifier configurationChangeNotifier) {
        this.configurationChangeNotifier = configurationChangeNotifier;
    }

    void setDifferentiator(Differentiator<ByteBuffer> differentiator) {
        this.differentiator = differentiator;
    }

    void setWatchService(WatchService watchService) {
        this.watchService = watchService;
    }

    boolean targetFileChanged() {
        logger.trace("Attempting to acquire watch key");
        Optional<WatchKey> watchKey = ofNullable(watchService.poll());
        logger.trace("Watch key acquire with value {}", watchKey);
        boolean targetChanged = watchKey.map(WatchKey::pollEvents).orElse(emptyList()).stream()
                .anyMatch(watchEvent -> ENTRY_MODIFY == watchEvent.kind()
                        && ((WatchEvent<Path>) watchEvent)
                                .context()
                                .equals(configFilePath.getName(configFilePath.getNameCount() - 1)));
        logger.debug("Target file changed: {}", targetChanged);
        watchKey.map(WatchKey::reset).filter(valid -> !valid).ifPresent(valid -> {
            logger.error("Unable to reinitialize file system watcher.");
            throw new IllegalStateException("Unable to reinitialize file system watcher.");
        });
        logger.trace("Watch key has been reset successfully");
        return targetChanged;
    }

    private void checkConfigFileLocationCorrectness(BootstrapProperties properties, Path configFile) {
        Path flowConfigFile = Path.of(properties.getProperty(MiNiFiProperties.NIFI_MINIFI_FLOW_CONFIG.getKey()))
                .toAbsolutePath();
        Path rawFlowConfigFile =
                flowConfigFile.getParent().resolve(getBaseName(flowConfigFile.toString()) + RAW_EXTENSION);
        if (flowConfigFile.equals(configFile) || rawFlowConfigFile.equals(configFile)) {
            throw new IllegalStateException("File ingestor config file (" + CONFIG_FILE_PATH_KEY
                    + ") must point to a different file than MiNiFi flow config file and raw flow config file");
        }
    }

    private WatchService initializeWatcher(Path filePath) {
        try {
            WatchService fileSystemWatcher = FileSystems.getDefault().newWatchService();
            Path watchDirectory = filePath.getParent();
            watchDirectory.register(fileSystemWatcher, ENTRY_MODIFY);
            logger.trace("Watch service registered for {}", watchDirectory);
            return fileSystemWatcher;
        } catch (IOException ioe) {
            throw new IllegalStateException("Unable to initialize a file system watcher for the path " + filePath, ioe);
        }
    }

    private Supplier<IllegalArgumentException> unableToFindDifferentiatorExceptionSupplier(String differentiator) {
        return () -> new IllegalArgumentException("Property, " + DIFFERENTIATOR_KEY + ", has value " + differentiator
                + " which does not correspond to any in the FileChangeIngestor Map:"
                + DIFFERENTIATOR_CONSTRUCTOR_MAP.keySet());
    }
}
