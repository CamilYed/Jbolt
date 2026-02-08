package github.com.camilyed.jbolt.testing.dsl;

import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.domain.execution.HttpRequest;
import github.com.camilyed.jbolt.domain.execution.HttpResponse;
import org.assertj.core.api.AbstractAssert;

import java.util.Map;
import java.util.Optional;

public class DomainDSL {

    // --- HttpRequest Builder ---
    public static class HttpRequestBuilder {
        private String url = "https://api.jbolt.com";
        private HttpMethod method = HttpMethod.GET;
        private Map<String, String> headers = Map.of();
        private Optional<String> body = Optional.empty();

        public static HttpRequestBuilder aRequest() {
            return new HttpRequestBuilder();
        }

        public HttpRequestBuilder withUrl(String url) {
            this.url = url;
            return this;
        }

        public HttpRequestBuilder withMethod(HttpMethod method) {
            this.method = method;
            return this;
        }

        public HttpRequestBuilder withHeaders(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public HttpRequestBuilder withBody(String body) {
            this.body = Optional.ofNullable(body);
            return this;
        }

        public HttpRequest build() {
            return new HttpRequest(url, method, headers, body);
        }
    }

    // --- HttpResponse Builder ---
    public static class HttpResponseBuilder {
        private int statusCode = 200;
        private String body = "";
        private Map<String, String> headers = Map.of();
        private long durationMillis = 0L;

        public static HttpResponseBuilder aResponse() {
            return new HttpResponseBuilder();
        }

        public HttpResponseBuilder withStatus(int code) {
            this.statusCode = code;
            return this;
        }

        public HttpResponseBuilder withBody(String body) {
            this.body = body;
            return this;
        }

        public HttpResponseBuilder withHeaders(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public HttpResponseBuilder withDuration(long durationMillis) {
            this.durationMillis = durationMillis;
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(statusCode, body, headers, durationMillis);
        }
    }

    // --- HttpResponse Assert ---
    public static HttpResponseAssert assertThatResponse(HttpResponse actual) {
        return new HttpResponseAssert(actual);
    }

    public static class HttpResponseAssert extends AbstractAssert<HttpResponseAssert, HttpResponse> {

        public HttpResponseAssert(HttpResponse actual) {
            super(actual, HttpResponseAssert.class);
        }

        public HttpResponseAssert isSuccessful() {
            isNotNull();
            if (!actual.isSuccessful()) {
                failWithMessage("Expected response to be successful but status code was <%s>", actual.statusCode());
            }
            return this;
        }

        public HttpResponseAssert isNotSuccessful() {
            isNotNull();
            if (actual.isSuccessful()) {
                failWithMessage("Expected response NOT to be successful but status code was <%s>", actual.statusCode());
            }
            return this;
        }

        public HttpResponseAssert hasStatusCode(int expected) {
            isNotNull();
            if (actual.statusCode() != expected) {
                failWithMessage("Expected status code <%s> but was <%s>", expected, actual.statusCode());
            }
            return this;
        }

        public HttpResponseAssert hasBody(String expected) {
            isNotNull();
            if (!actual.body().equals(expected)) {
                failWithMessage("Expected body <%s> but was <%s>", expected, actual.body());
            }
            return this;
        }

        public HttpResponseAssert hasHeaders(Map<String, String> expected) {
            isNotNull();
            if (!actual.headers().equals(expected)) {
                failWithMessage("Expected headers <%s> but were <%s>", expected, actual.headers());
            }
            return this;
        }

        public HttpResponseAssert hasDuration(long expected) {
            isNotNull();
            if (actual.durationMillis() != expected) {
                failWithMessage("Expected duration <%s> but was <%s>", expected, actual.durationMillis());
            }
            return this;
        }
    }
}
