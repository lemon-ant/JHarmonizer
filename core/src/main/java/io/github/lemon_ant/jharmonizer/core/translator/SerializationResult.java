package io.github.lemon_ant.jharmonizer.core.translator;

import java.util.Objects;
import lombok.NonNull;
import lombok.Value;

@Value
public class SerializationResult {
    @NonNull
    SerializationStatistic serializationStatistic;

    @NonNull
    String serializedSourceCode;

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SerializationResult) obj;
        return Objects.equals(this.serializedSourceCode, that.serializedSourceCode)
                && Objects.equals(this.serializationStatistic, that.serializationStatistic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serializedSourceCode, serializationStatistic);
    }
}
