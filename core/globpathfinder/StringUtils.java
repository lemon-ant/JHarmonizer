package io.github.lemon_ant.globpathfinder;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
class StringUtils {

    @NonNull
    static <TProcessResult> Set<TProcessResult> processNormalizedStrings(
            @NonNull Collection<String> strings, @NonNull Function<String, TProcessResult> processor) {
        return strings.stream()
                .map(org.apache.commons.lang3.StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .map(processor)
                .collect(toUnmodifiableSet());
    }
}
