package github.com.camilyed.jbolt.domain.execution;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Domain model representing an immutable HTTP Request.
 * This record encapsulates all data required to execute a network call.
 *
 * @param url     The destination URL (must not be null).
 * @param method  The HTTP strategy to be used (must not be null).
 * @param headers A map of request headers (must not be null).
 * @param body    The optional request payload (must not be null).
 */
public record HttpRequest(
        String url,
        HttpMethod method,
        Map<String, String> headers,
        Optional<String> body
) {
    /**
     * Compact constructor for domain validation.
     * Ensures that the request is created in a valid state (fail-fast).
     *
     * @throws NullPointerException if any of the mandatory fields are null.
     */
    public HttpRequest {
        Objects.requireNonNull(url, "URL cannot be null");
        Objects.requireNonNull(method, "Method cannot be null");
        Objects.requireNonNull(headers, "Headers cannot be null");
        Objects.requireNonNull(body, "Body optional cannot be null");
    }
}

