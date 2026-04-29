package io.github.lemon_ant.jharmonizer.core.e2e;

import java.net.URI;

class UriFieldInitializerStringForwardReferenceRegressionSample {
    private static final URI HTTP_LOCALHOST_URI = URI.create("http://localhost/");
    private static final String PARAMETER_NAME = "search";
    private static final String RESOURCES_PATH_SEGMENT = "resources";
    private static final URI HTTP_LOCALHOST_RESOURCES_URI =
            URI.create(String.format("%s%s", HTTP_LOCALHOST_URI, RESOURCES_PATH_SEGMENT));
    private static final URI HTTP_LOCALHOST_QUERY_URI =
            URI.create(String.format("%s?%s", HTTP_LOCALHOST_RESOURCES_URI, PARAMETER_NAME));

    public static void main(String[] args) {
        if (!HTTP_LOCALHOST_URI.equals(URI.create("http://localhost/"))
                || !"search".equals(PARAMETER_NAME)
                || !"resources".equals(RESOURCES_PATH_SEGMENT)
                || !HTTP_LOCALHOST_RESOURCES_URI.equals(URI.create("http://localhost/resources"))
                || !HTTP_LOCALHOST_QUERY_URI.equals(URI.create("http://localhost/resources?search"))) {
            throw new IllegalStateException("Unexpected field values after initialization:"
                    + " HTTP_LOCALHOST_URI=" + HTTP_LOCALHOST_URI
                    + ", PARAMETER_NAME=" + PARAMETER_NAME
                    + ", RESOURCES_PATH_SEGMENT=" + RESOURCES_PATH_SEGMENT
                    + ", HTTP_LOCALHOST_RESOURCES_URI=" + HTTP_LOCALHOST_RESOURCES_URI
                    + ", HTTP_LOCALHOST_QUERY_URI=" + HTTP_LOCALHOST_QUERY_URI);
        }
    }
}
