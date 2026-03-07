package github.com.camilyed.jbolt.ui.model;

import github.com.camilyed.jbolt.application.execution.RequestExecutionService;
import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.domain.execution.HttpResponse;
import github.com.camilyed.jbolt.testing.dsl.fakes.FakeHttpEngine;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(vm.statusText.get()).isEqualTo("200");
            assertThat(vm.statusClass.get()).isEqualTo("success");
            assertThat(vm.responseBody.get()).contains("\"ok\" : true");
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
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(vm.responseBody.get()).isEqualTo("[Empty Response]");
            assertThat(vm.statusText.get()).isEqualTo("204");
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
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(vm.responseBody.get()).isEqualTo(invalidJson);
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
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(vm.responseBody.get()).contains(rootMessage);
            assertThat(vm.statusText.get()).isEqualTo("ERROR");
            assertThat(vm.statusClass.get()).isEqualTo("danger");
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
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(vm.responseBody.get()).contains(errorMsg);
        });
    }
}