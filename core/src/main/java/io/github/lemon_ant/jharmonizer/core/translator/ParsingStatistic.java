package io.github.lemon_ant.jharmonizer.core.translator;

import lombok.Value;

@Value
public class ParsingStatistic {
    long originalSourceCodeLength;
    int parsedMembersCount;
    int parsedRootTypesCount;
    int parsedTypesTotalCount;
    long parsingTimeInNanos;
}
