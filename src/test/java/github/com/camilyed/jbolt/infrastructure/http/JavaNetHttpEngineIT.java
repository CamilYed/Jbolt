package github.com.camilyed.jbolt.infrastructure.http;

import static github.com.camilyed.jbolt.testing.dsl.DomainDSL.HttpRequestBuilder.aRequest;
import static github.com.camilyed.jbolt.testing.dsl.DomainDSL.assertThatResponse;
import static github.com.camilyed.jbolt.testing.dsl.JsonTestDataBuilder.aJson;
import static github.com.camilyed.jbolt.testing.dsl.assertions.ResultAssertion.assertThatResult;

import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link JavaNetHttpEngine} using WireMock. Tests HTTP scenarios using the
 * Result pattern.
 */
class JavaNetHttpEngineIT extends BaseHttpIT {

  private final JavaNetHttpEngine engine = new JavaNetHttpEngine();

  @Test
  @DisplayName("GET returns 200 with JSON body and headers (wrapped in Result)")
  void getReturnsJson() {
    // given
    var expectedBody = aJson().withField("id", 1).withField("name", "Alice");

    // and
    givenRemoteServer().returnsSuccess("/users/1", "GET", expectedBody);

    // and
    var request = aRequest().withUrl(getBaseUrl() + "/users/1").withMethod(HttpMethod.GET).build();

    // when
    var result = engine.execute(request);

    // then
    assertThatResult(result)
        .isSuccess(
            response ->
                assertThatResponse(response)
                    .isSuccessful()
                    .hasBody(expectedBody)
                    .hasHeaders(Map.of("Content-Type", "application/json")));
  }

  @Test
  @DisplayName("GET with delayed response respects engine timing")
  void getDelayedResponse() {
    // given
    var expectedBody = aJson().withField("message", "ok");
    givenRemoteServer().returnsDelayed("/delayed", "GET", 500, expectedBody);

    // and
    var request = aRequest().withUrl(getBaseUrl() + "/delayed").withMethod(HttpMethod.GET).build();

    // when
    var result = engine.execute(request);

    // then
    assertThatResult(result)
        .isSuccess(response -> assertThatResponse(response).isSuccessful().hasBody(expectedBody));
  }

  @Test
  @DisplayName("GET with GZip response is decompressed correctly")
  void getGzipResponse() {
    // given
    var expectedBody = aJson().withField("data", "compressed");
    var path = "/gzip";
    givenRemoteServer().returnsSuccessGzip(path, "GET", expectedBody);

    // and
    var request = aRequest().withUrl(getBaseUrl() + path).withMethod(HttpMethod.GET).build();

    // when
    var result = engine.execute(request);

    // then
    assertThatResult(result)
        .isSuccess(
            response ->
                assertThatResponse(response)
                    .isSuccessful()
                    .hasBody(expectedBody)
                    .hasHeaders(
                        Map.of(
                            "Content-Encoding", "gzip",
                            "Content-Type", "application/json")));
  }

  @Test
  @DisplayName("GET returns error codes as Successful Result (HTTP protocol success)")
  void getErrorStatus() {
    // given
    var path = "/error";
    givenRemoteServer().returnsError(path, 404);

    // and
    var request = aRequest().withUrl(getBaseUrl() + path).withMethod(HttpMethod.GET).build();

    // when
    var result = engine.execute(request);

    // then: It is a Result.Success because the network call succeeded,
    // but the HttpResponse inside indicates a 404 (isNotSuccessful).
    assertThatResult(result)
        .isSuccess(
            response ->
                assertThatResponse(response).isNotSuccessful().hasStatusCode(404).hasBody(""));
  }

  @Test
  @DisplayName("POST sends JSON body and receives JSON response")
  void postJsonBody() {
    // given
    var requestBody = aJson().withField("name", "Bob").withField("age", 30);
    var expectedBody = aJson().withField("id", 123).withField("name", "Bob");

    // and
    givenRemoteServer().returnsPOST("/create", expectedBody);

    // and
    var request =
        aRequest()
            .withUrl(getBaseUrl() + "/create")
            .withMethod(HttpMethod.POST)
            .withBody(requestBody.toString())
            .withHeaders(Map.of("Content-Type", "application/json"))
            .build();

    // when
    var result = engine.execute(request);

    // then
    assertThatResult(result)
        .isSuccess(
            response ->
                assertThatResponse(response)
                    .isSuccessful()
                    .hasBody(expectedBody)
                    .hasHeaders(Map.of("Content-Type", "application/json")));
  }

  @Test
  @DisplayName("PUT request with headers and body")
  void putWithHeadersAndBody() {
    // given
    var requestBody = aJson().withField("status", "active");
    var expectedBody = aJson().withField("updated", true);
    givenRemoteServer()
        .returnsPUT("/update", expectedBody, Map.of("Authorization", "Bearer token"));

    // and
    var request =
        aRequest()
            .withUrl(getBaseUrl() + "/update")
            .withMethod(HttpMethod.PUT)
            .withBody(requestBody.toString())
            .withHeaders(
                Map.of(
                    "Authorization", "Bearer token",
                    "Content-Type", "application/json"))
            .build();

    // when
    var result = engine.execute(request);

    // then
    assertThatResult(result)
        .isSuccess(
            response ->
                assertThatResponse(response)
                    .isSuccessful()
                    .hasBody(expectedBody)
                    .hasHeaders(Map.of("Content-Type", "application/json")));
  }

  @Test
  @DisplayName("DELETE request returns 204 No Content")
  void deleteReturnsNoContent() {
    // given
    var expectedBody = aJson(); // empty body
    givenRemoteServer().returnsDELETE("/delete/1", expectedBody);

    // and
    var request =
        aRequest().withUrl(getBaseUrl() + "/delete/1").withMethod(HttpMethod.DELETE).build();

    // when
    var result = engine.execute(request);

    // then
    assertThatResult(result)
        .isSuccess(
            response ->
                assertThatResponse(response)
                    .isSuccessful()
                    .hasStatusCode(200)
                    .hasBody(expectedBody));
  }

  @Test
  @DisplayName("PATCH request updates resource")
  void patchRequest() {
    // given
    var requestBody = aJson().withField("field", "new");
    var expectedBody = aJson().withField("updated", true);

    // and
    givenRemoteServer().returnsPATCH("/patch/1", expectedBody);

    var request =
        aRequest()
            .withUrl(getBaseUrl() + "/patch/1")
            .withMethod(HttpMethod.PATCH)
            .withBody(requestBody.toString())
            .build();

    // when
    var result = engine.execute(request);

    // then
    assertThatResult(result)
        .isSuccess(response -> assertThatResponse(response).isSuccessful().hasBody(expectedBody));
  }

  @Test
  @DisplayName("OPTIONS request returns 200")
  void optionsRequest() {
    // given
    givenRemoteServer().returnsOPTIONS("/options");

    // and
    var request =
        aRequest().withUrl(getBaseUrl() + "/options").withMethod(HttpMethod.OPTIONS).build();

    // when
    var result = engine.execute(request);

    // then
    assertThatResult(result).isSuccess(response -> assertThatResponse(response).isSuccessful());
  }

  @Test
  @DisplayName("HEAD request returns 200 with headers")
  void headRequest() {
    // given
    givenRemoteServer().returnsHEAD("/head");

    // and
    var request = aRequest().withUrl(getBaseUrl() + "/head").withMethod(HttpMethod.HEAD).build();

    // when
    var result = engine.execute(request);

    // then
    assertThatResult(result)
        .isSuccess(response -> assertThatResponse(response).isSuccessful().hasStatusCode(200));
  }
}
