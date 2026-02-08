package github.com.camilyed.jbolt.domain.execution;

import java.util.Map;

/**
 * Domain model representing an immutable HTTP Response.
 * Contains the result data returned from the execution engine.
 *
 * @param statusCode     The HTTP status code (e.g., 200, 404).
 * @param body           The response payload as a string.
 * @param headers        The response headers received from the server.
 * @param durationMillis The time taken to receive the response in milliseconds.
 */
public record HttpResponse(
        int statusCode,
        String body,
        Map<String, String> headers,
        long durationMillis
) {
    /**
     * Checks if the response indicates a successful operation.
     * Logic is based on the standard 2xx Success status code range.
     *
     * @return true if statusCode is between 200 and 299 inclusive.
     */
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }
}
