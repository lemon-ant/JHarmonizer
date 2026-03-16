package io.github.lemon_ant.jharmonizer.core.translator;

import java.util.Objects;
import lombok.NonNull;
import lombok.Value;

/**
 * Result of serializing a sorted Spoon AST back to Java source code.
 * Bundles the serialized source string with the associated timing and size statistics.
 */
@Value
public class SerializationResult {
    @NonNull
    SerializationStatistic serializationStatistic;

    @NonNull
    String serializedSrcCode;

    /**
     * Checks whether this serialization result matches another object.
     * @param obj the obj
     * @return {@code true} if the check succeeds; otherwise {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SerializationResult) obj;
        return Objects.equals(this.serializedSrcCode, that.serializedSrcCode)
                && Objects.equals(this.serializationStatistic, that.serializationStatistic);
    }

    /**
     * Returns the hash code of this serialization result.
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(serializedSrcCode, serializationStatistic);
    }
}
