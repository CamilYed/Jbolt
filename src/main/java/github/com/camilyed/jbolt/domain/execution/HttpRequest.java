package github.com.camilyed.jbolt.domain.execution;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Domain model representing an immutable HTTP Request. This record encapsulates all data required
 * to execute a network call.
 *
 * @param url The destination URL (must not be null).
 * @param method The HTTP strategy to be used (must not be null).
 * @param headers A map of request headers (must not be null).
 * @param body The optional request payload (must not be null).
 */
public record HttpRequest(
    String url, HttpMethod method, Map<String, String> headers, Optional<String> body) {
  /**
   * Compact constructor for domain validation. Ensures that the request is created in a valid state
   * (fail-fast).
   *
   * @throws NullPointerException if any of the mandatory fields are null.
   */
  public HttpRequest {
    Objects.requireNonNull(url, "URL cannot be null");
    Objects.requireNonNull(method, "Method cannot be null");
    Objects.requireNonNull(headers, "Headers cannot be null");
    Objects.requireNonNull(body, "Body optional cannot be null");
  }

  public static HttpRequestBuilder builder() {
    return HttpRequestBuilder.aRequest();
  }

  /**
   * Fluent Builder for {@link HttpRequest} to ensure immutable object creation with sensible
   * defaults and easy-to-use API.
   */
  public static final class HttpRequestBuilder {
    private String url;
    private HttpMethod method = HttpMethod.GET;
    private Map<String, String> headers = new HashMap<>();
    private Optional<String> body = Optional.empty();

    private HttpRequestBuilder() {}

    /**
     * Creates a new instance of the builder.
     *
     * @return a new HttpRequestBuilder
     */
    public static HttpRequestBuilder aRequest() {
      return new HttpRequestBuilder();
    }

    /**
     * Sets the destination URL.
     *
     * @param url the destination URL
     * @return the current builder instance
     */
    public HttpRequestBuilder withUrl(String url) {
      this.url = url;
      return this;
    }

    /**
     * Sets the HTTP method.
     *
     * @param method the {@link HttpMethod} to use
     * @return the current builder instance
     */
    public HttpRequestBuilder withMethod(HttpMethod method) {
      this.method = method;
      return this;
    }

    /**
     * Replaces current headers with the provided map.
     *
     * @param headers map of HTTP headers
     * @return the current builder instance
     */
    public HttpRequestBuilder withHeaders(Map<String, String> headers) {
      this.headers = headers == null ? new HashMap<>() : new HashMap<>(headers);
      return this;
    }

    /**
     * Adds or updates a single header.
     *
     * @param name header name
     * @param value header value
     * @return the current builder instance
     */
    public HttpRequestBuilder withHeader(String name, String value) {
      this.headers.put(name, value);
      return this;
    }

    /**
     * Sets the request body. If the value is null, body is set to Optional.empty().
     *
     * @param body the request payload
     * @return the current builder instance
     */
    public HttpRequestBuilder withBody(String body) {
      this.body = Optional.ofNullable(body);
      return this;
    }

    /**
     * Builds the {@link HttpRequest} instance. Performs final validation via the record's compact
     * constructor.
     *
     * @return a new HttpRequest instance
     */
    public HttpRequest build() {
      return new HttpRequest(url, method, Collections.unmodifiableMap(headers), body);
    }
  }
}
