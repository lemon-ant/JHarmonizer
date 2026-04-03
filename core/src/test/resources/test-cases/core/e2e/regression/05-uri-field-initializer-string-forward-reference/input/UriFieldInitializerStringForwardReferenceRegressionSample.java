package io.github.lemon_ant.jharmonizer.core.e2e;

import java.net.URI;

class UriFieldInitializerStringForwardReferenceRegressionSample {
    private static final String RESOURCES_PATH_SEGMENT = "resources";

    private static final String PARAMETER_NAME = "search";

    private static final URI HTTP_LOCALHOST_URI = URI.create("http://localhost/");

    private static final URI HTTP_LOCALHOST_RESOURCES_URI =
            URI.create(String.format("%s%s", HTTP_LOCALHOST_URI, RESOURCES_PATH_SEGMENT));

    private static final URI HTTP_LOCALHOST_QUERY_URI =
            URI.create(String.format("%s?%s", HTTP_LOCALHOST_RESOURCES_URI, PARAMETER_NAME));
}
