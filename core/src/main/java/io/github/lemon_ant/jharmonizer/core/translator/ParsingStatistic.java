package io.github.lemon_ant.jharmonizer.core.translator;

import lombok.Value;

/**
 * Timing and source statistics collected during a single source-file parsing pass,
 * including source-length and UTF-8 byte size.
 */
@Value
public class ParsingStatistic {
    long originalSrcCodeLength;
    long originalSrcCodeSizeInBytes;
    int parsedMembersCount;
    int parsedRootTypesCount;
    int parsedTypesTotalCount;
    long parsingTimeInNanos;
}
