package github.com.camilyed.jbolt;

import static github.com.camilyed.jbolt.testing.dsl.JsonTestDataBuilder.aJson;
import static org.testfx.assertions.api.Assertions.assertThat;

import github.com.camilyed.jbolt.infrastructure.http.BaseHttpIT;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

/**
 * End-to-end proof that the app wires request execution through to the response panel. Uses {@link
 * BaseHttpIT}'s local WireMock server instead of a live third-party API - a real network dependency
 * here would make this test flaky by construction (see CLAUDE.md's testing philosophy: hermetic,
 * deterministic tests, no reliance on services this project doesn't control).
 */
@ExtendWith(ApplicationExtension.class)
class AppTest extends BaseHttpIT {

  @Start
  public void start(final Stage stage) throws Exception {
    final var app = new App();
    app.init();
    app.start(stage);
  }

  @Test
  void testApiRequestFlow(final FxRobot robot) throws TimeoutException {
    // given
    givenRemoteServer().returnsGET("/get", aJson().withField("url", getBaseUrl() + "/get"));
    WaitForAsyncUtils.waitForFxEvents();
    final var urlField = robot.lookup("#urlField").queryAs(TextField.class);

    // when
    robot.interact(
        () -> {
          urlField.requestFocus();
          urlField.setText(getBaseUrl() + "/get");
        });

    // then
    assertThat(urlField.getText()).isEqualTo(getBaseUrl() + "/get");

    // and
    final var sendBtn =
        robot.lookup(".button").queryAllAs(Button.class).stream()
            .filter(b -> "SEND".equals(b.getText()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("SEND button not found in the current scene"));

    // when
    robot.interact(
        () -> {
          sendBtn.requestFocus();
          sendBtn.fire();
        });

    // then
    WaitForAsyncUtils.waitFor(
        5,
        TimeUnit.SECONDS,
        () -> {
          final var responseArea = robot.lookup("#responseArea").queryAs(TextArea.class);
          final var content = responseArea.getText();
          return content.contains("200") || content.contains(getBaseUrl() + "/get");
        });

    final var responseArea = robot.lookup("#responseArea").queryAs(TextArea.class);
    assertThat(responseArea.getText()).contains(getBaseUrl() + "/get");
  }
}
