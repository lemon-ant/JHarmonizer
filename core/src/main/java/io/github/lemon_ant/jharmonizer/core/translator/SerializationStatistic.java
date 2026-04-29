package io.github.lemon_ant.jharmonizer.core.translator;

import lombok.Value;

/**
 * Timing and source-length statistics collected during a single AST-to-source serialization pass.
 */
@Value
public class SerializationStatistic {
    long processingTimeInNanos;
    long serializedCodeLength;

    /**
     * Creates a new SerializationStatistic.
     * @param serializedCodeLength the serialized code length
     * @param processingTimeInNanos the processing time in nanos
     */
    public SerializationStatistic(long serializedCodeLength, long processingTimeInNanos) {
        this.serializedCodeLength = serializedCodeLength;
        this.processingTimeInNanos = processingTimeInNanos;
    }

    @Override
    public String toString() {
        return "SerializationStatistic[" + "serializedCodeLength="
                + serializedCodeLength + ", " + "processingTimeInNanos="
                + processingTimeInNanos + ']';
    }
}
