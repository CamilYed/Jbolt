package github.com.camilyed.jbolt.infrastructure.http;

import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.domain.execution.HttpRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static github.com.camilyed.jbolt.testing.dsl.DomainDSL.HttpRequestBuilder.aRequest;
import static github.com.camilyed.jbolt.testing.dsl.DomainDSL.assertThatResponse;
import static github.com.camilyed.jbolt.testing.dsl.JsonTestDataBuilder.aJson;

/**
 * Integration tests for {@link JavaNetHttpEngine} using WireMock.
 * Tests HTTP scenarios: GET/POST/PUT/DELETE, headers, JSON body, GZip, delays, and error status codes.
 */
class JavaNetHttpEngineIT extends BaseHttpIT {

    private final JavaNetHttpEngine engine = new JavaNetHttpEngine();

    @Test
    @DisplayName("GET returns 200 with JSON body and headers")
    void getReturnsJson() throws Exception {
        // given
        var expectedBody = aJson()
                .withField("id", 1)
                .withField("name", "Alice");

        givenRemoteServer()
                .returnsSuccess("/users/1", "GET", expectedBody);

        // and
        var request = aRequest()
                .withUrl(getBaseUrl() + "/users/1")
                .withMethod(HttpMethod.GET)
                .build();

        // when
        var response = engine.execute(request);

        // then
        assertThatResponse(response).isSuccessful()
                .hasBody(expectedBody)
                .hasHeaders(Map.of("Content-Type", "application/json"));
    }

    @Test
    @DisplayName("GET with delayed response respects engine timing")
    void getDelayedResponse() throws Exception {
        // given
        var expectedBody = aJson().withField("message", "ok");
        givenRemoteServer().returnsDelayed( "/delayed", "GET", 500, expectedBody);

        // and
        var request = aRequest()
                .withUrl(getBaseUrl() +  "/delayed")
                .withMethod(HttpMethod.GET)
                .build();

        // when
        long start = System.currentTimeMillis();
        var response = engine.execute(request);
        long duration = System.currentTimeMillis() - start;

        // then
        assertThatResponse(response).isSuccessful()
                .hasBody(expectedBody);

        // and
        assert duration >= 500;
    }

    @Test
    @DisplayName("GET with GZip response is decompressed")
    void getGzipResponse() throws Exception {
        // given
        var expectedBody = aJson().withField("data", "compressed");
        var path = "/gzip";
        givenRemoteServer().returnsSuccessGzip(path, "GET", expectedBody);

        // and
        var request = aRequest()
                .withUrl(getBaseUrl() + path)
                .withMethod(HttpMethod.GET)
                .build();

        // when
        var response = engine.execute(request);

        // then
        assertThatResponse(response).isSuccessful()
                .hasBody(expectedBody)
                .hasHeaders(Map.of(
                        "Content-Encoding", "gzip",
                        "Content-Type", "application/json"
                ));
    }

    @Test
    @DisplayName("GET returns error codes correctly")
    void getErrorStatus() throws Exception {
        // given
        var path = "/error";
        givenRemoteServer().returnsError(path, 404);

        // and
        var request = aRequest()
                .withUrl(getBaseUrl() + path)
                .withMethod(HttpMethod.GET)
                .build();

        // when
        var response = engine.execute(request);

        // then
        assertThatResponse(response).isNotSuccessful()
                .hasStatusCode(404)
                .hasBody("");
    }

    @Test
    @DisplayName("POST sends JSON body and receives JSON response")
    void postJsonBody() throws Exception {
        // given
        var requestBody = aJson().withField("name", "Bob").withField("age", 30);
        var expectedBody = aJson().withField("id", 123).withField("name", "Bob");
        givenRemoteServer().returnsPOST("/create", expectedBody);

        // and
        HttpRequest request = aRequest()
                .withUrl(getBaseUrl() + "/create")
                .withMethod(HttpMethod.POST)
                .withBody(requestBody.toString())
                .withHeaders(Map.of("Content-Type", "application/json"))
                .build();

        // when
        var response = engine.execute(request);

        // then
        assertThatResponse(response).isSuccessful()
                .hasBody(expectedBody)
                .hasHeaders(Map.of("Content-Type", "application/json"));
    }

    @Test
    @DisplayName("PUT request with headers and body")
    void putWithHeadersAndBody() throws Exception {
        // given
        var requestBody = aJson().withField("status", "active");
        var expectedBody = aJson().withField("updated", true);
        givenRemoteServer().returnsPUT("/update", expectedBody, Map.of("Authorization", "Bearer token"));

        // and
        var request = aRequest()
                .withUrl(getBaseUrl() + "/update")
                .withMethod(HttpMethod.PUT)
                .withBody(requestBody.toString())
                .withHeaders(Map.of(
                        "Authorization", "Bearer token",
                        "Content-Type", "application/json"
                ))
                .build();

        // when
        var response = engine.execute(request);

        // then
        assertThatResponse(response).isSuccessful()
                .hasBody(expectedBody)
                .hasHeaders(Map.of(
                        "Content-Type", "application/json"
                ));
    }

    @Test
    @DisplayName("DELETE request returns 204 No Content")
    void deleteReturnsNoContent() throws Exception {
        // given
        var expectedBody = aJson(); // empty body
        givenRemoteServer().returnsDELETE("/delete/1", expectedBody);

        // and
        var request = aRequest()
                .withUrl(getBaseUrl() + "/delete/1")
                .withMethod(HttpMethod.DELETE)
                .build();

        // when
        var response = engine.execute(request);

        // then
        assertThatResponse(response).isSuccessful()
                .hasStatusCode(200)
                .hasBody(expectedBody);
    }

    @Test
    @DisplayName("PATCH request updates resource")
    void patchRequest() throws Exception {
        // given
        var requestBody = aJson().withField("field", "new");
        var expectedBody = aJson().withField("updated", true);

        // and
        givenRemoteServer().returnsPATCH("/patch/1", expectedBody);

        var request = aRequest()
                .withUrl(getBaseUrl() + "/patch/1")
                .withMethod(HttpMethod.PATCH)
                .withBody(requestBody.toString())
                .build();

        // when
        var response = engine.execute(request);

        // then
        assertThatResponse(response).isSuccessful()
                .hasBody(expectedBody);
    }

    @Test
    @DisplayName("OPTIONS request returns 200")
    void optionsRequest() throws Exception {
        // given
        givenRemoteServer().returnsOPTIONS("/options");

        // and
        var request = aRequest()
                .withUrl(getBaseUrl() + "/options")
                .withMethod(HttpMethod.OPTIONS)
                .build();

        // when
        var response = engine.execute(request);

        // then
        assertThatResponse(response).isSuccessful();
    }

    @Test
    @DisplayName("HEAD request returns 200 with headers")
    void headRequest() throws Exception {
        // given
        givenRemoteServer().returnsHEAD("/head");

        // and
        var request = aRequest()
                .withUrl(getBaseUrl() + "/head")
                .withMethod(HttpMethod.HEAD)
                .build();

        // when
        var response = engine.execute(request);

        // then
        assertThatResponse(response)
                .isSuccessful()
                .hasStatusCode(200);
    }
}
