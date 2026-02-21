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
            // Toolkit already initialized
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
            assertThat(vm.timeText.get()).contains("100 ms");
            assertThat(vm.responseBody.get()).contains("\"ok\" : true");
            assertThat(fakeEngine.lastRequest().url()).isEqualTo("http://test.com");
        });
    }

    @Test
    @DisplayName("Should show error message on failure")
    void shouldHandleFailure() {
        // given
        final var errorMsg = "Network Error";
        fakeEngine.willFail(new RuntimeException(errorMsg));

        vm.url.set("http://fail.com");

        // when
        vm.sendRequest();

        // then
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(vm.statusText.get()).isEqualTo("ERROR");
            assertThat(vm.statusClass.get()).isEqualTo("danger");
            assertThat(vm.responseBody.get()).contains(errorMsg);
        });
    }
}