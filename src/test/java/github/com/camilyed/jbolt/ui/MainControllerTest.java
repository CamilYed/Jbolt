package github.com.camilyed.jbolt.ui;

import github.com.camilyed.jbolt.testing.dsl.fakes.FakeUiMessageService;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MainControllerTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (final IllegalStateException _) {
            // Already initialized
        }
    }

    @Test
    @DisplayName("should trigger error message when FXML loading fails")
    void shouldHandleLoadingFailure() {
        // given
        final var fakeUiService = new FakeUiMessageService();

        // Provide a factory that crashes to force a Failure in Result.of()
        final ControllerFactory failingFactory = _ -> {
            throw new RuntimeException("Simulated Factory Crash");
        };

        final var controller = new MainController(failingFactory, fakeUiService);

        // when
        controller.openNewTab();

        // then
        assertThat(fakeUiService.wasErrorShown()).isTrue();
        assertThat(fakeUiService.getCapturedError().technicalCode()).isEqualTo("UI-LOAD-001");
        assertThat(fakeUiService.getCapturedError().title()).contains("View Loading Failed");
    }

    @Test
    @DisplayName("should handle missing FXML resource safely")
    void shouldHandleMissingResource() {
        // given
        final var fakeUiService = new FakeUiMessageService();
        final ControllerFactory dummyFactory = _ -> null;

        final var controller = new MainController(dummyFactory, fakeUiService);

        // when
        // If we point to a non-existent path internally or force a null resource
        controller.openNewTab();

        // then
        assertThat(fakeUiService.wasErrorShown()).isTrue();
        assertThat(fakeUiService.getCapturedError().cause()).isInstanceOf(java.io.IOException.class);
    }
}