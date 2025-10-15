package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import java.util.Locale;
import java.util.Objects;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
class EnumDeserializerUtil {
    @NonNull
    static <T extends Enum<T>> T deserialize(@NonNull Class<T> enumClass, @NonNull String value) {
        String trimmedValue = Objects.requireNonNull(StringUtils.trimToNull(value));
        String enumName = trimmedValue.toUpperCase(Locale.ENGLISH).replace('-', '_');
        return Enum.valueOf(enumClass, enumName);
    }
}
