package io.github.lemon_ant.jharmonizer.core.translator;

import lombok.Value;

/**
 * Timing and size statistics collected during a single source-file parsing pass.
 */
@Value
public class ParsingStatistic {
    long originalSrcCodeLength;
    int parsedMembersCount;
    int parsedRootTypesCount;
    int parsedTypesTotalCount;
    long parsingTimeInNanos;
}
