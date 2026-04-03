import java.net.URI;

class UriFieldInitializerStringForwardReferenceRegressionSample {
    private static final String HTTP_SCHEME = "http";
    private static final String LOCALHOST = "localhost";
    private static final URI HTTP_LOCALHOST_URI = URI.create(String.format("%s://%s/", HTTP_SCHEME, LOCALHOST));
    private static final String RESOURCES_PATH_SEGMENT = "resources";
    private static final URI HTTP_LOCALHOST_RESOURCES_URI =
            URI.create(String.format("%s%s", HTTP_LOCALHOST_URI, RESOURCES_PATH_SEGMENT));
}
