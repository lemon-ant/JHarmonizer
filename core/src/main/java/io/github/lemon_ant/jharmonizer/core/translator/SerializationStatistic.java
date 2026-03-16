package io.github.lemon_ant.jharmonizer.core.translator;

import java.util.Objects;
import lombok.Value;

/**
 * Timing and size statistics collected during a single AST-to-source serialization pass.
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

    /**
     * Checks whether this serialization statistic matches another object.
     * @param obj the obj
     * @return {@code true} if the check succeeds; otherwise {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SerializationStatistic) obj;
        return this.serializedCodeLength == that.serializedCodeLength
                && this.processingTimeInNanos == that.processingTimeInNanos;
    }

    /**
     * Returns the hash code of this serialization statistic.
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(serializedCodeLength, processingTimeInNanos);
    }

    /**
     * Returns the string representation of this serialization statistic.
     *
     * @return the string representation of this statistic
     */
    @Override
    public String toString() {
        return "SerializationStatistic[" + "serializedCodeLength="
                + serializedCodeLength + ", " + "processingTimeInNanos="
                + processingTimeInNanos + ']';
    }
}
