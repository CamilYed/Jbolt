package github.com.camilyed.jbolt.domain.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static github.com.camilyed.jbolt.testing.dsl.DomainDSL.HttpRequestBuilder.aRequest;
import static github.com.camilyed.jbolt.testing.dsl.DomainDSL.HttpResponseBuilder.aResponse;
import static github.com.camilyed.jbolt.testing.dsl.DomainDSL.assertThatResponse;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Unit tests for Domain Execution models to ensure 100% coverage of logic and validation.
 */
class DomainExecutionTest {

    @Test
    @DisplayName("HttpRequest should fail when URL is missing")
    void httpRequestRequiresUrl() {
        // expect
        assertThatThrownBy(() ->
                aRequest()
                        .withUrl(null)
                        .build()
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("URL cannot be null");
    }

    @Test
    @DisplayName("HttpRequest should fail when method is missing")
    void httpRequestRequiresMethod() {
        // expect
        assertThatThrownBy(() ->
                aRequest()
                        .withMethod(null)
                        .build()
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Method cannot be null");
    }

    @ParameterizedTest(name = "HTTP {0} should be successful")
    @ValueSource(ints = {200, 201, 204})
    @DisplayName("2xx HTTP responses are successful")
    void http2xxResponsesAreSuccessful(int statusCode) {
        // when
        HttpResponse response = aResponse()
                .withStatus(statusCode)
                .build();

        // then
        assertThatResponse(response).isSuccessful();
    }

    @ParameterizedTest(name = "HTTP {0} should NOT be successful")
    @ValueSource(ints = {300})
    @DisplayName("3xx HTTP responses are NOT successful")
    void http3xxResponsesAreNotSuccessful(int statusCode) {
        // when
        HttpResponse response = aResponse()
                .withStatus(statusCode)
                .build();

        // then
        assertThatResponse(response).isNotSuccessful();
    }

    @ParameterizedTest(name = "HTTP {0} should NOT be successful")
    @ValueSource(ints = {400, 404})
    @DisplayName("4xx HTTP responses are NOT successful")
    void http4xxResponsesAreNotSuccessful(int statusCode) {
        // when
        HttpResponse response = aResponse()
                .withStatus(statusCode)
                .build();

        // then
        assertThatResponse(response).isNotSuccessful();
    }

    @ParameterizedTest(name = "HTTP {0} should NOT be successful")
    @ValueSource(ints = {500})
    @DisplayName("5xx HTTP responses are NOT successful")
    void http5xxResponsesAreNotSuccessful(int statusCode) {
        // when
        HttpResponse response = aResponse()
                .withStatus(statusCode)
                .build();

        // then
        assertThatResponse(response).isNotSuccessful();
    }

    @Test
    @DisplayName("HttpRequest should fail when headers are null")
    void httpRequestRequiresHeaders() {
        // expect
        assertThatThrownBy(() ->
                aRequest()
                        .withHeaders(null)
                        .build()
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Headers cannot be null");
    }

    @Test
    @DisplayName("HttpRequest should allow null body (treated as empty)")
    void httpRequestAllowsNullBody() {
        // when
        HttpRequest request = aRequest()
                .withBody(null)
                .build();

        // then
        assertThat(request.body()).isEmpty();
    }

    @Test
    @DisplayName("HttpResponse should correctly store all provided data")
    void httpResponseDataIntegrityTest() {
        // given
        Map<String, String> headers = Map.of("Content-Type", "application/json");
        String body = "{\"status\":\"ok\"}";
        int status = 200;
        long duration = 250L;

        // when
        HttpResponse response = aResponse()
                .withStatus(status)
                .withBody(body)
                .withHeaders(headers)
                .withDuration(duration)
                .build();

        // then
        assertThatResponse(response)
                .hasStatusCode(status)
                .hasBody(body)
                .hasHeaders(headers)
                .hasDuration(duration);
    }

}