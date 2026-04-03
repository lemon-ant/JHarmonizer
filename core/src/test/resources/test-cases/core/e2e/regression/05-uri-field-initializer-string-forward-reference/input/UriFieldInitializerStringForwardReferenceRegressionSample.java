package org.apache.nifi.web.client;

import org.apache.nifi.web.client.api.HttpUriBuilder;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StandardHttpUriBuilderTest {
    private static final String HTTP_SCHEME = "http";

    private static final String LOCALHOST = "localhost";

    private static final int PORT = 8080;

    private static final String ENCODED_PATH = "/resources/search";

    private static final String ENCODED_PATH_WITH_TRAILING_SEPARATOR = "/resources/search/";

    private static final String PATH_WITH_SPACES_ENCODED = "/resources/%20separated%20search";

    private static final String BUCKETS_PATH_SEGMENT = "buckets";

    private static final String FILES_PATH_SEGMENT = "files";

    private static final String RESOURCES_PATH_SEGMENT = "resources";

    private static final String RESOURCES_PATH_SEGMENT_SEPARATED = "resources|separated";

    private static final String RESOURCES_PATH_SEGMENT_SEPARATED_ENCODED = "resources%7Cseparated";

    private static final String PARAMETER_NAME = "search";

    private static final String PARAMETER_VALUE = "terms";

    private static final String SECOND_PARAMETER_NAME = "refresh";

    private static final String SECOND_PARAMETER_VALUE = "{0}";

    private static final String SECOND_PARAMETER_VALUE_ENCODED = "%7B0%7D";

    private static final String PARAMETER_NAME_AND_VALUE = String.format("%s=%s", PARAMETER_NAME, PARAMETER_VALUE);

    private static final URI HTTP_LOCALHOST_URI = URI.create(
        String.format("%s://%s/", HTTP_SCHEME, LOCALHOST)
    );

    private static final URI HTTP_LOCALHOST_PORT_URI = URI.create(
        String.format("%s://%s:%d/", HTTP_SCHEME, LOCALHOST, PORT)
    );

    private static final URI HTTP_LOCALHOST_PORT_ENCODED_PATH_URI = URI.create(
        String.format("%s://%s:%d%s", HTTP_SCHEME, LOCALHOST, PORT, ENCODED_PATH)
    );

    private static final URI HTTP_LOCALHOST_PORT_ENCODED_PATH_WITH_SPACES_URI = URI.create(
        String.format("%s://%s:%d%s", HTTP_SCHEME, LOCALHOST, PORT, PATH_WITH_SPACES_ENCODED)
    );

    private static final URI HTTP_LOCALHOST_PORT_ENCODED_PATH_WITH_SPACES_AND_SEGMENTS_URI = URI.create(
        String.format("%s://%s:%d%s/%s/%s", HTTP_SCHEME, LOCALHOST, PORT, PATH_WITH_SPACES_ENCODED, BUCKETS_PATH_SEGMENT, FILES_PATH_SEGMENT)
    );

    private static final URI HTTP_LOCALHOST_PORT_ENCODED_PATH_WITH_TRAILING_SEPARATOR_AND_SEGMENTS_URI = URI.create(
        String.format("%s://%s:%d%s%s/%s", HTTP_SCHEME, LOCALHOST, PORT, ENCODED_PATH_WITH_TRAILING_SEPARATOR, BUCKETS_PATH_SEGMENT, FILES_PATH_SEGMENT)
    );

    private static final URI HTTP_LOCALHOST_RESOURCES_URI = URI.create(
        String.format("%s%s", HTTP_LOCALHOST_URI, RESOURCES_PATH_SEGMENT)
    );

    private static final URI HTTP_LOCALHOST_RESOURCES_SEPARATED_URI = URI.create(
        String.format("%s%s", HTTP_LOCALHOST_URI, RESOURCES_PATH_SEGMENT_SEPARATED_ENCODED)
    );

    private static final URI HTTP_LOCALHOST_QUERY_URI = URI.create(
        String.format("%s?%s", HTTP_LOCALHOST_RESOURCES_URI, PARAMETER_NAME_AND_VALUE)
    );

    private static final URI HTTP_LOCALHOST_QUERY_REPEATED_URI = URI.create(
        String.format("%s?%s&%s", HTTP_LOCALHOST_RESOURCES_URI, PARAMETER_NAME_AND_VALUE, PARAMETER_NAME_AND_VALUE)
    );

    private static final URI HTTP_LOCALHOST_QUERY_SECOND_PARAMETER_URI = URI.create(
        String.format("%s?%s&%s", HTTP_LOCALHOST_RESOURCES_URI, PARAMETER_NAME_AND_VALUE, SECOND_PARAMETER_NAME)
    );

    private static final URI HTTP_LOCALHOST_QUERY_PARAMETER_VALUE_URI = URI.create(
        String.format("%s?%s=%s", HTTP_LOCALHOST_RESOURCES_URI, SECOND_PARAMETER_NAME, SECOND_PARAMETER_VALUE_ENCODED)
    );

    private static final URI HTTP_LOCALHOST_QUERY_EMPTY_VALUE_URI = URI.create(
        String.format("%s?%s", HTTP_LOCALHOST_RESOURCES_URI, PARAMETER_NAME)
    );
}
