package io.github.lemon_ant.jharmonizer.core.parser;

import java.util.Objects;
import lombok.Value;

@Value
public class SerializationStatistic {
    long serializedCodeLength;
    long processingTimeInNanos;

    public SerializationStatistic(long serializedCodeLength, long processingTimeInNanos) {
        this.serializedCodeLength = serializedCodeLength;
        this.processingTimeInNanos = processingTimeInNanos;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SerializationStatistic) obj;
        return this.serializedCodeLength == that.serializedCodeLength
                && this.processingTimeInNanos == that.processingTimeInNanos;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serializedCodeLength, processingTimeInNanos);
    }

    @Override
    public String toString() {
        return "SerializationStatistic[" + "serializedCodeLength="
                + serializedCodeLength + ", " + "processingTimeinNanos="
                + processingTimeInNanos + ']';
    }
}
