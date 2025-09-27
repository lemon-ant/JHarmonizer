package io.github.lemon_ant.jharmonizer.core.translator;

import lombok.Value;

@Value
public class ParsingStatistic {
    long originalSourceCodeLength;
    int parsedRootTypesCount;
    int parsedTypesTotalCount;
    int parsedMembersCount;
    long parsingTimeInNanos;
}
