package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.time.format.DateTimeFormatter;

public class FieldInitializerCompileTimeConstantExclusionFixture {

    private static final String ALTERNATE_FORMAT_WITHOUT_MILLIS = "yyyy/MM/dd HH:mm:ss";
    private static final DateTimeFormatter ALTERNATE_FORMATTER_WITHOUT_MILLIS =
            DateTimeFormatter.ofPattern(ALTERNATE_FORMAT_WITHOUT_MILLIS);
}
