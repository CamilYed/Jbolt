package github.com.camilyed.jbolt.ui.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import github.com.camilyed.jbolt.application.execution.RequestExecutionService;
import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.domain.execution.HttpResponse;
import github.com.camilyed.jbolt.testing.dsl.fakes.FakeHttpEngine;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequestTabViewModelTest {

  private final FakeHttpEngine fakeEngine = new FakeHttpEngine();
  private final RequestExecutionService service = new RequestExecutionService(fakeEngine);
  private RequestTabViewModel vm;

  @BeforeAll
  static void initJavaFX() {
    try {
      Platform.startup(() -> {});
    } catch (final IllegalStateException _) {
      // Already initialized
    }
  }

  @BeforeEach
  void setUp() {
    vm = new RequestTabViewModel(service);
  }

  @Test
  @DisplayName("Should update status text and class on successful request")
  void shouldHandleSuccess() {
    // given
    final var jsonBody = "{\"ok\":true}";
    final var response = new HttpResponse(200, jsonBody, Map.of(), 100);
    fakeEngine.willReturn(response);

    vm.url.set("http://test.com");
    vm.method.set(HttpMethod.GET);

    // when
    vm.sendRequest();

    // then
    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(vm.statusText.get()).isEqualTo("200");
              assertThat(vm.statusClass.get()).isEqualTo("success");
              assertThat(vm.responseBody.get()).contains("\"ok\" : true");
            });
  }

  @Test
  @DisplayName("Should expose the parsed JSON tree for an object response")
  void shouldExposeParsedJsonForObjectResponse() {
    // given
    final var response = new HttpResponse(200, "{\"id\":1,\"name\":\"Mascara\"}", Map.of(), 100);
    fakeEngine.willReturn(response);
    vm.url.set("http://test.com");

    // when
    vm.sendRequest();

    // then
    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(vm.responseJson.get()).isNotNull();
              assertThat(vm.responseJson.get().isObject()).isTrue();
              assertThat(vm.responseJson.get().get("name").asText()).isEqualTo("Mascara");
            });
  }

  @Test
  @DisplayName("Should not expose a parsed JSON tree for a scalar response")
  void shouldNotExposeParsedJsonForScalarResponse() {
    // given
    final var response = new HttpResponse(200, "42", Map.of(), 100);
    fakeEngine.willReturn(response);
    vm.url.set("http://test.com");

    // when
    vm.sendRequest();

    // then
    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(vm.statusText.get()).isEqualTo("200");
              assertThat(vm.responseJson.get().isValueNode()).isTrue();
            });
  }

  @Test
  @DisplayName("Should expose the parsed XML document for an xml response")
  void shouldExposeParsedXmlForXmlResponse() {
    // given
    final var response =
        new HttpResponse(200, "<person><name>Alice</name></person>", Map.of(), 100);
    fakeEngine.willReturn(response);
    vm.url.set("http://test.com");

    // when
    vm.sendRequest();

    // then
    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(vm.responseXml.get()).isNotNull();
              assertThat(vm.responseXml.get().getDocumentElement().getTagName())
                  .isEqualTo("person");
              assertThat(vm.responseJson.get()).isNull();
            });
  }

  @Test
  @DisplayName("Should detect xml via the Content-Type header even when the body looks ambiguous")
  void shouldDetectXmlViaContentTypeHeader() {
    // given
    final var headers = Map.of("Content-Type", "text/xml; charset=utf-8");
    final var response = new HttpResponse(200, "<a>1</a>", headers, 100);
    fakeEngine.willReturn(response);
    vm.url.set("http://test.com");

    // when
    vm.sendRequest();

    // then
    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(vm.responseXml.get()).isNotNull();
              assertThat(vm.responseJson.get()).isNull();
            });
  }

  @Test
  @DisplayName("Should handle malformed XML and fall back to raw text")
  void shouldHandleMalformedXml() {
    // given
    final var invalidXml = "<person><name>Alice</name>"; // Missing closing </person>
    final var response = new HttpResponse(200, invalidXml, Map.of(), 50);
    fakeEngine.willReturn(response);
    vm.url.set("http://invalid.com");

    // when
    vm.sendRequest();

    // then
    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(vm.responseBody.get()).isEqualTo(invalidXml);
              assertThat(vm.responseXml.get()).isNull();
              assertThat(vm.responseJson.get()).isNull();
            });
  }

  @Test
  @DisplayName("Should handle empty response body gracefully")
  void shouldHandleEmptyBody() {
    // given
    final var response = new HttpResponse(204, "", Map.of(), 50);
    fakeEngine.willReturn(response);
    vm.url.set("http://empty.com");

    // when
    vm.sendRequest();

    // then
    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(vm.responseBody.get()).isEqualTo("[Empty Response]");
              assertThat(vm.statusText.get()).isEqualTo("204");
              assertThat(vm.responseJson.get()).isNull();
              assertThat(vm.responseXml.get()).isNull();
            });
  }

  @Test
  @DisplayName("Should handle malformed JSON and fall back to raw text")
  void shouldHandleMalformedJson() {
    // given
    final var invalidJson = "{ \"bad_json\": true "; // Missing closing brace
    final var response = new HttpResponse(200, invalidJson, Map.of(), 50);
    fakeEngine.willReturn(response);
    vm.url.set("http://invalid.com");

    // when
    vm.sendRequest();

    // then
    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(vm.responseBody.get()).isEqualTo(invalidJson);
              assertThat(vm.responseJson.get()).isNull();
            });
  }

  @Test
  @DisplayName("Should extract root cause message from nested exceptions")
  void shouldHandleNestedException() {
    // given
    final var rootMessage = "Connection refused";
    final var wrapper = new RuntimeException("Outer", new Exception(rootMessage));
    fakeEngine.willFail(wrapper);
    vm.url.set("http://fail.com");

    // when
    vm.sendRequest();

    // then
    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(vm.responseBody.get()).contains(rootMessage);
              assertThat(vm.statusText.get()).isEqualTo("ERROR");
              assertThat(vm.statusClass.get()).isEqualTo("danger");
              assertThat(vm.responseJson.get()).isNull();
            });
  }

  @Test
  @DisplayName("Should show error message on direct exception without cause")
  void shouldHandleSimpleException() {
    // given
    final var errorMsg = "Direct Failure";
    fakeEngine.willFail(new RuntimeException(errorMsg));
    vm.url.set("http://fail.com");

    // when
    vm.sendRequest();

    // then
    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(vm.responseBody.get()).contains(errorMsg);
            });
  }
}
